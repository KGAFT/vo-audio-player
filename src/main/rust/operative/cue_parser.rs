use crate::util::text_decoder::binary_to_text;
use cue::cd::CD;
use cue::cd_text::PTI;
use jni::JNIEnv;
use jni::objects::{JObject, JObjectArray, JString, JValue};
use jni::sys::jobject;
use std::collections::HashMap;
use std::fs;
use std::fs::File;
use std::path::{Path, PathBuf};
use std::time::Duration;


#[derive(Clone)]
pub struct CueSong {
    offset: Duration,
    title: String,
    duration: Duration,
    performer: String,
}

pub struct CueAlbum {
    title: String,
    path: String,
    duration: Duration,
    songs: Vec<CueSong>,
    performer: String,
}

pub fn parse_cue_file(path: &str) -> Option<Vec<CueAlbum>> {

    let file = fs::read(Path::new(path));
    if file.is_err() {
        eprintln!("Failed to read cue file: {}", path);
        return None;
    }
    let text = binary_to_text(file.unwrap().as_slice());

    println!("trying to open cue");
    let cue = CD::parse(text);
    if cue.is_err() {
        eprintln!("Failed to parse cue file: {}", path);
        return None;
    }
    let cue = cue.unwrap();
    println!("Cue readed");
    let mut cd_text = cue.get_cdtext();
    //Some of this shit maybe artist, due to how people writing this
    println!("trying to read artist");
    let artist = cd_text.read(PTI::Performer).unwrap_or(
        cd_text.read(PTI::Songwriter).unwrap_or(
            cd_text.read(PTI::Arranger).unwrap_or(
                cd_text
                    .read(PTI::Composer)
                    .unwrap_or("unknown artist".to_string()),
            ),
        ),
    );
    println!("artist read");
    let main_album = cd_text.read(PTI::Title).unwrap_or("untitled".to_string());
    let mut tracks_map: HashMap<String, Vec<CueSong>> = HashMap::new();
    for i in 0..cue.get_track_count() {
        let track = cue.get_track(i).unwrap();
        cd_text = track.get_cdtext();
        /*
        let artist = cd_text.read(PTI::Performer).unwrap_or(
            cd_text.read(PTI::Songwriter).unwrap_or(
                cd_text.read(PTI::Arranger).unwrap_or(
                    cd_text
                        .read(PTI::Composer)
                        .unwrap_or(artist.clone()),
                ),
            ),
        );

         */
        println!("trying to get filename");
        let filename = track.get_filename();
        println!("filename obtained");
        let mut target_container =
            if let Some(container) = tracks_map.get_mut(&filename) {
                container
            } else {
                tracks_map.insert(filename.clone(), Vec::new());
                tracks_map.get_mut(&filename).unwrap()
            };
        println!("trying to get offset");
        let off = frames_to_duration(track.get_start());
        println!("offset obtained");
        println!("trying to get duration");
        let dur = frames_to_duration(track.get_length().unwrap_or(0));
        println!("duration obtained");
        let song_info = CueSong{
            offset: off,
            title: cd_text.read(PTI::Title).unwrap_or("untitled".to_string()),
            duration: dur,
            performer: artist.clone(),
        };
        target_container.push(song_info);
    }
    if tracks_map.len() == 1{
        let mut key = String::new();
        for info in tracks_map.iter_mut(){
            key = info.0.clone();
            break
        }
        let tracks = tracks_map.remove(&key).unwrap();
        let album_name = if main_album.eq("untitled") {
            key.clone()
        } else {
            main_album.clone()
        };
        let duration = calc_album_duration(tracks.as_slice());
        let album = CueAlbum{
            title: album_name,
            path: key,
            duration,
            songs: tracks,
            performer: artist.clone(),
        };
        Some(vec![album])
    } else {
        let mut albums_res: Vec<CueAlbum> = Vec::new();
        tracks_map.iter_mut().for_each(|(filename, songs)|{
            let album_name = main_album.clone() +" ("+filename+")";
            let duration = calc_album_duration(songs.as_slice());
            let album = CueAlbum{
                title: album_name,
                path: filename.clone(),
                duration,
                songs: songs.clone(),
                performer: artist.clone(),
            };
            albums_res.push(album);
        });
        Some(albums_res)
    }
}

fn calc_album_duration(songs: &[CueSong]) -> Duration{
    let mut duration = 0;
    for x in songs.iter() {
        duration+=x.duration.as_millis()
    };
    if duration == 0{
        for x in songs.iter() {
            duration+=x.offset.as_millis()
        };
    }
    Duration::from_millis(duration as u64)
}

fn frames_to_duration(frames: i64) -> Duration {
    Duration::from_millis((frames * 1000 )as u64 / 75 as u64)
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
        let res = indices.first();
        if res.is_none() {
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
pub unsafe fn cue_album_to_java(
    env: &mut JNIEnv,
    album: &CueAlbum,
) -> jni::errors::Result<jobject> {
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
pub unsafe fn cue_album_from_java(
    env: &mut JNIEnv,
    j_album: JObject,
) -> jni::errors::Result<CueAlbum> {
    unsafe {
        let j_title: JString = env
            .get_field(&j_album, "title", "Ljava/lang/String;")?
            .l()?
            .into();
        let j_path: JString = env
            .get_field(&j_album, "path", "Ljava/lang/String;")?
            .l()?
            .into();
        let j_perf: JString = env
            .get_field(&j_album, "performer", "Ljava/lang/String;")?
            .l()?
            .into();
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
    let s_title: JString = env
        .get_field(&j_song, "title", "Ljava/lang/String;")?
        .l()?
        .into();
    let s_perf: JString = env
        .get_field(&j_song, "performer", "Ljava/lang/String;")?
        .l()?
        .into();
    let s_offset = env.get_field(&j_song, "offset", "J")?.j()? as u64;
    let s_duration = env.get_field(&j_song, "duration", "J")?.j()? as u64;

    Ok(CueSong {
        offset: Duration::from_millis(s_offset),
        title: env.get_string(&s_title)?.into(),
        duration: Duration::from_millis(s_duration),
        performer: env.get_string(&s_perf)?.into(),
    })
}
