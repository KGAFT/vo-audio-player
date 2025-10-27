use gstreamer::StreamError::Format;
use rfd::FileDialog;

pub fn request_file() -> String{
    let path = FileDialog::new()
        .add_filter("Audio", &["m4a", "flac", "wv", "wav", "mp3", "dsd", "dsf"])
        .pick_file().unwrap();
    let binding = path.canonicalize().unwrap();
    let mut res = binding.to_string_lossy();
    #[cfg(target_os = "windows")]
    {
        return res.replace('\\', "/").to_string()[3..].to_string();
    }
    res.to_string()
}