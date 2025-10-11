use gst::prelude::*;
use gstreamer as gst;
use gstreamer::MessageView;
use std::io;
use std::io::Write;
use std::sync::atomic::AtomicBool;
use std::sync::atomic::Ordering::Relaxed;
use std::time::Duration;

pub struct AudioPlayer {
    playbin: gst::Element,
    playing: AtomicBool,
}

unsafe impl Send for AudioPlayer {}

unsafe impl Sync for AudioPlayer {}

impl AudioPlayer {
    pub fn new() -> Self {
        gst::init().expect("Failed to initialize GStreamer");

        let playbin = gst::ElementFactory::make("playbin")
            .property(
                "audio-sink",
                &gst::ElementFactory::make("autoaudiosink")
                    .build()
                    .expect("Could not create autoaudiosink"),
            )
            .build()
            .unwrap();

        Self {
            playbin,
            playing: AtomicBool::new(false),
        }
    }

    pub fn list_output_devices() -> Vec<(String, String)> {
        gst::init().ok(); // safe to call multiple times
        let monitor = gst::DeviceMonitor::new();
        monitor.add_filter(Some("Audio/Sink"), None::<&gst::Caps>);

        monitor.start().expect("Failed to start device monitor");
        let devices = monitor.devices();
        monitor.stop();

        devices
            .iter()
            .filter_map(|dev| {
                let name = dev.display_name();
                let device_class = dev.device_class();
                let device_str = dev
                    .properties()
                    .and_then(|props| {
                        Some(
                            props
                                .get::<&str>("device.name")
                                .ok()
                                .unwrap_or("Unknown")
                                .to_string(),
                        )
                    })
                    .unwrap();
                Some((name.to_string(), device_str))
            })
            .collect()
    }

    pub fn set_output_device(&self, device_name: &str) -> bool {
        let monitor = gst::DeviceMonitor::new();
        monitor.add_filter(Some("Audio/Sink"), None::<&gst::Caps>);
        monitor.start().ok();

        for dev in monitor.devices() {
            let name = dev.display_name();
            if name.contains(device_name) {
                if let Ok(sink) = dev.create_element(None) {
                    self.playbin.set_property("audio-sink", &sink);
                    println!("Set audio device to: {}", name);
                    monitor.stop();
                    return true;
                }
            }
        }

        monitor.stop();
        eprintln!("Could not find device matching '{}'", device_name);
        false
    }

    /// Load a new file and set it up
    pub fn set_uri(&self, filepath: &str) {
        let uri = if filepath.starts_with("http://")
            || filepath.starts_with("https://")
            || filepath.starts_with("file://")
        {
            filepath.to_string()
        } else {
            format!("file://{}", filepath)
        };

        self.playbin.set_property("uri", &uri);
    }

    /// Start playback
    pub fn play(&self) {
        self.playing.store(true, Relaxed);
        self.playbin
            .set_state(gst::State::Playing)
            .expect("Unable to set to Playing");
    }

    /// Pause playback
    pub fn pause(&self) {
        self.playing.store(false, Relaxed);
        self.playbin
            .set_state(gst::State::Paused)
            .expect("Unable to set to Paused");
    }

    pub fn is_playing(&self) -> bool {
        self.playing.load(Relaxed)
    }

    /// Seek within the track by percentage (0.0 = start, 1.0 = end)
    pub fn seek_percent(&self, percent: f64) -> bool {
        if let Some(duration) = self.duration() {
            let target = (duration.as_nanos() as f64 * percent).round() as u64;
            let time = gst::ClockTime::from_nseconds(target);

            let pipeline = self.playbin.clone().dynamic_cast::<gst::Pipeline>();
            if pipeline.is_err() {
                return false;
            }
            let pipeline = pipeline.unwrap();
            return pipeline
                .seek_simple(gst::SeekFlags::FLUSH | gst::SeekFlags::KEY_UNIT, time)
                .is_ok();
        }
        self.play();
        false
    }

    pub fn set_volume(&self, volume: f64) {
        let clamped = volume.clamp(0.0, 1.0);
        self.playbin.set_property("volume", clamped);
    }

    /// Get current playback volume (0.0 - 1.0)
    pub fn volume(&self) -> f64 {
        self.playbin.property::<f64>("volume")
    }

    /// Stop playback (set to Null state)
    pub fn stop(&self) {
        self.playbin.set_property("uri", "");
        self.playbin
            .set_state(gst::State::Ready)
            .expect("Unable to set to Ready");
    }

    pub fn poll_bus(&self) -> bool {
        if let Some(bus) = self.playbin.bus() {
            while let Some(msg) = bus.timed_pop(gst::ClockTime::from_mseconds(10)) {
                use gst::MessageView;

                match msg.view() {
                    MessageView::Eos(..) => {
                        println!("End of stream reached");
                        io::stdout().flush().unwrap();
                        return false; // signal playback ended
                    }
                    MessageView::Error(err) => {
                        eprintln!(
                            "Error from {:?}: {} ({:?})",
                            err.src()
                                .map(|s| s.path_string())
                                .unwrap_or_else(|| "Unknown".into()),
                            err.error(),
                            err.debug()
                        );
                        io::stdout().flush().unwrap();
                        return false; // stop playback loop
                    }
                    MessageView::StateChanged(state_changed) => {
                        if let Some(el) = msg.src() {
                            if el.eq(&self.playbin) {
                                println!(
                                    "Pipeline state changed: {:?} -> {:?}",
                                    state_changed.old(),
                                    state_changed.current()
                                );
                                io::stdout().flush().unwrap();
                            }
                        }
                    }
                    _ => { /* ignore other messages */ }
                }
            }
        }
        true
    }

    /// Current playback position
    pub fn position(&self) -> Option<Duration> {
        let pipeline = self.playbin.clone().dynamic_cast::<gst::Pipeline>().ok()?;

        pipeline
            .query_position::<gst::ClockTime>()
            .map(|pos| Duration::from_nanos(pos.nseconds()))
    }

    /// Total duration
    pub fn duration(&self) -> Option<Duration> {
        let pipeline = self.playbin.clone().dynamic_cast::<gst::Pipeline>().ok()?;
        pipeline
            .query_duration::<gst::ClockTime>()
            .map(|dur| Duration::from_nanos(dur.nseconds()))
    }
}
