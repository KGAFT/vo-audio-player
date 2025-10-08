use crate::j_objects::album::{Album, Library};
use bincode::config::{Configuration, Fixint, LittleEndian};
use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JList, JObject, JString, JValue};
use jni::sys::{jbyteArray, jobject};
use std::fs::File;
use std::io::{BufReader, BufWriter, Write};

pub static BINCODE_CONFIG: Configuration<LittleEndian, Fixint> = bincode::config::standard()
    .with_little_endian()
    .with_fixed_int_encoding()
    .with_no_limit();

unsafe fn albums_from_jni(env: &mut JNIEnv, albums: JObject) -> Library {
    let size: i32 = env
        .call_method(albums.as_ref(), "size", "()I", &[])
        .unwrap()
        .i()
        .unwrap();

    let mut albums_list = Vec::with_capacity(size as usize);

    for i in 0..size {
        // Get element at index i
        let element = env
            .call_method(
                albums.as_ref(),
                "get",
                "(I)Ljava/lang/Object;",
                &[JValue::Int(i)],
            )
            .unwrap()
            .l()
            .unwrap();
        let album = Album::java_album_to_rust(env, element);
        albums_list.push(album)
    }
    Library {
        albums: albums_list,
    }
}

unsafe fn serialize_albums(env: &mut JNIEnv, albums: JObject) -> Vec<u8> {
    let library = albums_from_jni(env, albums);
    bincode::serde::encode_to_vec(&library, BINCODE_CONFIG).expect("Failed to encode library")
}

unsafe fn library_to_jlist(env: &mut JNIEnv, mut library: Library) -> jobject{
    let arraylist_class = env.find_class("java/util/ArrayList").unwrap();

    // Create a new ArrayList instance
    let list_obj = env.new_object(arraylist_class, "()V", &[]).unwrap();

    // Add each element
    while let Some(album) = library.albums.pop() {
        let album = JObject::from_raw(album.rust_album_to_java(env));
        env.call_method(
            list_obj.as_ref(),
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&album)],
        )
            .unwrap();
    }

    list_obj.as_raw()
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_LibrarySerializer_serialize(
    mut env: JNIEnv,
    _class: JClass,
    albums: JObject,
) -> jbyteArray {
    let data = serialize_albums(&mut env, albums);
    env.byte_array_from_slice(data.as_slice())
        .expect("Failed to create array")
        .as_raw()
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_LibrarySerializer_deserialize(
    mut env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jobject {
    let data = env.convert_byte_array(data).unwrap();
    let mut library = bincode::serde::decode_from_slice::<
        Library,
        Configuration<LittleEndian, Fixint>,
    >(data.as_slice(), BINCODE_CONFIG)
    .expect("Failed to decode library")
    .0;
    library_to_jlist(&mut env, library)
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_LibrarySerializer_serializeAndSaveToFile(
    mut env: JNIEnv,
    _class: JClass,
    albums: JObject,
    destination: JString,
) {
    let binding = env.get_string(&destination).unwrap();
    let destination = binding.to_str().unwrap();
    let albums = albums_from_jni(&mut env, albums);
    let mut file = File::create(destination).unwrap();
    let mut writer = BufWriter::new(file);
    bincode::serde::encode_into_std_write(albums, &mut writer, BINCODE_CONFIG)
        .expect("Failed to serialize");
    writer.flush().unwrap();
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_kgaft_VoidAudioPlayer_Native_LibrarySerializer_deserializeFromFile(
    mut env: JNIEnv,
    _class: JClass,
    source: JString,
) -> jobject {
    let binding = env.get_string(&source).unwrap();
    let source = binding.to_str().unwrap();
    let mut file = File::open(source).unwrap();
    let mut reader = BufReader::new(file);
    let library: Library = bincode::serde::decode_from_std_read(&mut reader, BINCODE_CONFIG).unwrap();
    library_to_jlist(&mut env, library)
}
