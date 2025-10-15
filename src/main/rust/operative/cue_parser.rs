use std::time::Duration;
use jni::JNIEnv;
use jni::objects::{JObject, JObjectArray, JString, JValue};
use jni::sys::jobject;

pub struct CueSong{
    offset: Duration,
    title: String,
    duration: Duration,
    performer: String,

}

pub struct CueAlbum{
    title: String,
    path: String,
    duration: Duration,
    songs: Vec<CueSong>,
    performer: String
}

pub fn parse_cue_file(path: &str) -> Option<Vec<CueAlbum>>{
    let cue = rcue::parser::parse_from_file(path, true);
    if cue.is_err(){
        eprintln!("Parsing error: file: {}  error: {}", path,cue.unwrap_err().to_string());
        return None;
    }
    let mut cue = cue.unwrap();
    let performer_res = cue.performer.as_ref();
    let mut performer = String::new();
    if performer_res.is_some(){
        performer = performer_res.unwrap().clone();
    }
    let title_res = cue.performer;
    let mut title = String::new();
    if title_res.is_some() {
        title = title_res.unwrap();
    }
    let mut cue_albums = Vec::new();
    for mut file in cue.files.iter_mut(){
        let mut cue_album = CueAlbum{path: file.file.clone(), duration: Duration::from_millis(0), songs: Vec::with_capacity(file.tracks.len()),
            performer: performer.clone(), title: title.clone()};
        let mut album_dur: usize = 0;
        while !file.tracks.is_empty() {
            let track = file.tracks.remove(0);
            if let Some(start_index) = find_index(&track.indices){
                let mut track = CueSong{
                    offset: start_index,
                    title: track.title.unwrap_or("untitled".to_string()),
                    duration: Duration::from_millis(0),
                    performer: track.performer.unwrap_or(track.songwriter.unwrap_or(String::new()))
                };
                let mut duration = Duration::from_millis(0);
                if let Some(next_track) = file.tracks.get(0){
                    duration = find_index(&next_track.indices).unwrap_or(Duration::from_millis(0));
                }
                track.duration = duration;
                cue_album.songs.push(track);
                album_dur+=duration.as_millis() as usize;
            } else {
                continue;
            }
        }
        cue_album.duration = Duration::from_millis(album_dur as u64);
        cue_albums.push(cue_album);
    }
    Some(cue_albums)
}

fn find_index(indices: &Vec<(String, Duration)>) -> Option<Duration> {
    let mut start_index = Duration::from_millis(0);
    for x in indices.iter() {
        if x.0.contains("01") {
            start_index = x.1.clone();
            break;
        }
    }
    if start_index.is_zero() {
        let res =  indices.first();
        if res.is_none(){
            return None;
        }
        start_index = res.unwrap().1.clone();
    }
    Some(start_index)
}


/// Convert Rust CueSong → Java CueSong
fn cue_song_to_java(env: &mut JNIEnv, song: &CueSong) -> jni::errors::Result<jobject> {
    let song_class = env.find_class("com/kgaft/VoidAudioPlayer/Verbose/CueSong")?;

    let j_title = env.new_string(&song.title)?;
    let j_perf = env.new_string(&song.performer)?;

    let j_song = env.new_object(
        song_class,
        "(JLjava/lang/String;JLjava/lang/String;)V",
        &[
            JValue::Long(song.offset.as_millis() as i64),
            JValue::Object(&JObject::from(j_title)),
            JValue::Long(song.duration.as_millis() as i64),
            JValue::Object(&JObject::from(j_perf)),
        ],
    )?;

    Ok(j_song.as_raw())
}

/// Convert Rust CueAlbum → Java CueAlbum using ArrayList
pub unsafe fn cue_album_to_java(env: &mut JNIEnv, album: &CueAlbum) -> jni::errors::Result<jobject> {
    // Create ArrayList<CueSong>
    let arraylist_class = env.find_class("java/util/ArrayList")?;
    let arraylist_obj = env.new_object(&arraylist_class, "()V", &[])?;
    let add_method_id = env.get_method_id(&arraylist_class, "add", "(Ljava/lang/Object;)Z")?;

    for song in &album.songs {
        let j_song = cue_song_to_java(env, song)?;
        env.call_method_unchecked(
            &arraylist_obj,
            add_method_id,
            jni::signature::ReturnType::Primitive(jni::signature::Primitive::Boolean),
            &[JValue::Object(&JObject::from_raw(j_song)).as_jni()],
        )?;
    }

    // Build CueAlbum
    let album_class = env.find_class("com/kgaft/VoidAudioPlayer/Verbose/CueAlbum")?;
    let j_title = env.new_string(&album.title)?;
    let j_path = env.new_string(&album.path)?;
    let j_perf = env.new_string(&album.performer)?;

    let j_album = env.new_object(
        album_class,
        "(Ljava/lang/String;Ljava/lang/String;JLjava/util/List;Ljava/lang/String;)V",
        &[
            JValue::Object(&JObject::from(j_title)),
            JValue::Object(&JObject::from(j_path)),
            JValue::Long(album.duration.as_millis() as i64),
            JValue::Object(&arraylist_obj),
            JValue::Object(&JObject::from(j_perf)),
        ],
    )?;

    Ok(j_album.as_raw())
}

/// Convert Java CueAlbum → Rust CueAlbum
pub unsafe fn cue_album_from_java(env: &mut JNIEnv, j_album: JObject) -> jni::errors::Result<CueAlbum> {
    unsafe {
        let j_title: JString = env.get_field(&j_album, "title", "Ljava/lang/String;")?.l()?.into();
        let j_path: JString = env.get_field(&j_album, "path", "Ljava/lang/String;")?.l()?.into();
        let j_perf: JString = env.get_field(&j_album, "performer", "Ljava/lang/String;")?.l()?.into();
        let j_duration = env.get_field(&j_album, "duration", "J")?.j()? as u64;
        let j_songs_list = env.get_field(&j_album, "songs", "Ljava/util/List;")?.l()?;

        let title = env.get_string(&j_title)?.into();
        let path = env.get_string(&j_path)?.into();
        let performer = env.get_string(&j_perf)?.into();
        let duration = Duration::from_millis(j_duration);

        // Access songs from List<CueSong>
        let list_class = env.find_class("java/util/List")?;
        let size_method = env.get_method_id(&list_class, "size", "()I")?;
        let get_method = env.get_method_id(&list_class, "get", "(I)Ljava/lang/Object;")?;

        let size = env
            .call_method_unchecked(
                &j_songs_list,
                size_method,
                jni::signature::ReturnType::Primitive(jni::signature::Primitive::Int),
                &[],
            )?
            .i()? as i32;

        let mut songs = Vec::with_capacity(size as usize);
        for i in 0..size {
            let j_song = env
                .call_method_unchecked(
                    &j_songs_list,
                    get_method,
                    jni::signature::ReturnType::Object,
                    &[JValue::Int(i).as_jni()],
                )?
                .l()?;
            songs.push(cue_song_from_java(env, j_song)?);
        }

        Ok(CueAlbum {
            title,
            path,
            duration,
            songs,
            performer,
        })
    }
}

/// Convert Java CueSong → Rust CueSong
fn cue_song_from_java(env: &mut JNIEnv, j_song: JObject) -> jni::errors::Result<CueSong> {
    let s_title: JString = env.get_field(&j_song, "title", "Ljava/lang/String;")?.l()?.into();
    let s_perf: JString = env.get_field(&j_song, "performer", "Ljava/lang/String;")?.l()?.into();
    let s_offset = env.get_field(&j_song, "offset", "J")?.j()? as u64;
    let s_duration = env.get_field(&j_song, "duration", "J")?.j()? as u64;

    Ok(CueSong {
        offset: Duration::from_millis(s_offset),
        title: env.get_string(&s_title)?.into(),
        duration: Duration::from_millis(s_duration),
        performer: env.get_string(&s_perf)?.into(),
    })
}
