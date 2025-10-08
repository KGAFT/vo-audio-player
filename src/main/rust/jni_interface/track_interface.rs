use crate::j_objects::track::Track;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jobject;

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Verbose_Track_getTrackInfo(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jobject {
    let path = env.get_string(&path).unwrap();
    let info = Track::new(path.to_str().unwrap(), &mut env);
    info
}
