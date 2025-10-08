use crate::operative::audio_player::AudioPlayer;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jfloat, jlong};
use std::thread::sleep;
use std::time::Duration;

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_initializePlayer(
    env: JNIEnv,
    _class: JClass,
) -> jlong {
    let player = Box::from(AudioPlayer::new());
    Box::into_raw(player) as jlong
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_loadTrack(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    path: JString,
) {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    player.set_uri(env.get_string(&path).unwrap().to_str().unwrap())
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_seekTrack(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    percent: jfloat,
) -> jboolean {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    player.seek_percent(percent as f64) as jboolean
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_getTrackLength(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlong {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    player
        .duration()
        .unwrap_or_else(|| Duration::from_millis(0))
        .as_millis() as jlong
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_pollEventsMain(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    loop {
        player.poll_bus();
        sleep(Duration::from_millis(100));
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_getTrackPos(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlong {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    player
        .position()
        .unwrap_or_else(|| Duration::from_millis(0))
        .as_millis() as jlong
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_stop(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    player.stop();
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_setPlaying(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    playing: jboolean,
) {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    if playing != 0 {
        player.play()
    } else {
        player.pause()
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_Player_isPlaying(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    let player = handle as *mut AudioPlayer;
    let player = player.as_mut().unwrap();
    player.is_playing() as jboolean
}
