use crate::operative::dsd_player::DsdPlayer;
use byteorder::{BigEndian, LittleEndian, ReadBytesExt};
use std::io::ErrorKind;
use std::{
    fs::File,
    io::{self, Read, Seek, SeekFrom},
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, Ordering},
    },
    thread,
    time::Duration,
};
use crate::operative::audio_player::AudioPlayer;

pub mod j_objects;
pub mod jni_interface;
pub mod operative;
pub mod util;
fn main() {
    let mut player = AudioPlayer::new();
    let devices = AudioPlayer::list_output_devices();
    devices.iter().for_each(|device| {
        println!("{}, {}", device.0, device.1);
    });

    player.set_output_device(devices[0].0.as_str());

    /*
    let mut supported_devices = DsdPlayer::enumerate_supported_devices();
    supported_devices.iter().for_each(|device| {
        println!("Supported devices: {:?}", supported_devices);
    });
    let mut player = DsdPlayer::new(supported_devices[3].0.to_str().unwrap());

    player.load_new_track("/mnt/files2/Music/test.dsf");
    player.seek(0.99f64).unwrap();
    player.play_on_current_thread();

    player.load_new_track("/mnt/files2/Music/Alphaville - Forever Young (Remastered) (1984_2019) [LP] DSD128/Alphaville - Forever Young (Remastered) (1984_2019) [LP] DSD128.dff");
    println!("{}", player.get_current_position_percents());
    player.play_on_current_thread();

     */
}
