use crate::operative::audio_diag;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jstring;

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_FileDialog_requestMusicFile(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let path = audio_diag::request_file();
    env.new_string(path).unwrap().as_raw()
}
