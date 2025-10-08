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

pub mod j_objects;
pub mod jni_interface;
pub mod operative;
fn main() {
    let mut player = DsdPlayer::new("/mnt/files2/Music/test.dsf", "hw:2,0");
    player.seek(0.99f64).unwrap();
    player.play_on_current_thread();

    player.load_new_track("/mnt/files2/Music/Alphaville - Forever Young (Remastered) (1984_2019) [LP] DSD128/Alphaville - Forever Young (Remastered) (1984_2019) [LP] DSD128.dff");
    println!("{}", player.get_current_position_percents());
    player.play_on_current_thread();

}
