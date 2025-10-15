use crate::operative::cue_parser::{cue_album_to_java, parse_cue_file};
use crate::util::text_decoder::binary_to_text;
use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JObject, JString, JValue};
use jni::sys::{jbyte, jbyteArray, jobject, jstring};

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_CueParser_parseCueFile(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jobject {
    let res = parse_cue_file(env.get_string(&file_path).unwrap().to_str().unwrap());
    if res.is_none() {
        return jobject::default();
    }
    let mut res = res.unwrap();
    let arraylist_class = env.find_class("java/util/ArrayList").unwrap();

    // Create a new ArrayList instance
    let list_obj = env.new_object(arraylist_class, "()V", &[]).unwrap();

    // Add each element
    while !res.is_empty() {
        let album = res.remove(0);
        let album_j = JObject::from_raw(cue_album_to_java(&mut env, &album).unwrap());
        env.call_method(
            list_obj.as_ref(),
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&album_j)],
        )
        .unwrap();
    }
    list_obj.as_raw()
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_CueParser_decodeText(
    mut env: JNIEnv,
    _class: JClass,
    data: jbyteArray,
) -> jstring {
    let bytes: Vec<u8> = env.convert_byte_array(&JByteArray::from_raw(data)).unwrap();
    let decoded = binary_to_text(bytes.as_slice());
    env.new_string(decoded).unwrap().as_raw()
}
