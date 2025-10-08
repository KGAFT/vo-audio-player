use jni::objects::{JClass, JString, JObject, JValue};
use jni::sys::{jboolean, jfloat, jlong};
use jni::JNIEnv;
use crate::operative::dsd_player::DsdPlayer;

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_enumerateSupportedDevices(
    mut env: JNIEnv,
    _class: JClass,
) -> JObject {
    let devices = DsdPlayer::enumerate_supported_devices();
    let arraylist_class = env.find_class("java/util/ArrayList").unwrap();
    let j_devices = env.new_object(arraylist_class, "()V", &[]).unwrap();
    devices.iter().for_each(|device|{
        let str = device.0.clone().into_string().unwrap()+"/:/"+device.1.to_str().unwrap();
        let j_string = env.new_string(str).unwrap();
        env.call_method(
            j_devices.as_ref(),
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&j_string)],
        )
            .unwrap();
    });
    j_devices
}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_initializePlayer(
    env: JNIEnv,
    _class: JClass,
    index: i32,
) -> jlong {

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_loadTrack(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    path: JString,
){

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_playOnCurrentThread(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
){

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_seekTrack(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    percent: jfloat,
) -> jboolean{

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_getTrackLength(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlong{

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_getTrackPos(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jfloat{

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_setPlaying(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    is_playing: jboolean,
){

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_isPlaying(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean{

}

#[no_mangle]
pub extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_PlayerDsd_stop(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
){

}
