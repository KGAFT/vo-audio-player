use crate::operative::dsd_player::DsdPlayer;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jfloat, jlong, jobject};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_enumerateSupportedDevices(
    mut env: JNIEnv,
    _class: JClass,
) -> jobject {
    let devices = DsdPlayer::enumerate_supported_devices();
    let arraylist_class = env.find_class("java/util/ArrayList").unwrap();
    let j_devices = env.new_object(arraylist_class, "()V", &[]).unwrap();
    devices.iter().for_each(|device| {
        let str = device.0.clone().into_string().unwrap() + "/:/" + device.1.to_str().unwrap();
        let j_string = env.new_string(str).unwrap();
        env.call_method(
            j_devices.as_ref(),
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&j_string)],
        )
        .unwrap();
    });
    j_devices.as_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_initializePlayer(
    mut env: JNIEnv,
    _class: JClass,
    device_name: JString,
) -> jlong {
    let device_name = env.get_string(&device_name).unwrap();
    let dsd_player = DsdPlayer::new(device_name.to_str().unwrap());
    Box::into_raw(Box::from(dsd_player)) as jlong
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_destroyPlayer(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong
){
    unsafe {
        drop(Box::from_raw(handle as *mut DsdPlayer));
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_loadTrack(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    path: JString,
) {
    let player = unsafe{(handle as *mut DsdPlayer).as_mut().unwrap()};
    let path = env.get_string(&path).unwrap();
    let path_str = path.to_str().unwrap();
    println!("{}", path_str);
    player.load_new_track(path_str);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_playOnCurrentThread(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    let player = unsafe{(handle as *mut DsdPlayer).as_mut().unwrap()};
    player.play_on_current_thread();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_seekTrack(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    percent: jfloat,
) -> jboolean {
    let mut player = unsafe{(handle as *mut DsdPlayer).as_mut().unwrap()};
    player.seek(percent as f64).is_ok() as jboolean
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_getTrackLength(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlong {
    0
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_getTrackPos(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jfloat {
    let player = unsafe{(handle as *mut DsdPlayer).as_mut().unwrap()};
    player.get_current_position_percents() as jfloat
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_setPlaying(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    is_playing: jboolean,
) {
    let player = unsafe{(handle as *mut DsdPlayer).as_mut().unwrap()};
    if is_playing != 0 {
        player.play();
    } else {
        player.pause();
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_isPlaying(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    let player = unsafe{(handle as *mut DsdPlayer).as_mut().unwrap()};
    player.is_playing() as jboolean
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_stop(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    let player = unsafe{(handle as *mut DsdPlayer).as_mut().unwrap()};
    player.stop();
}
