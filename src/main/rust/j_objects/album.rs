use crate::j_objects::track::Track;
use jni::JNIEnv;
use jni::objects::{JByteArray, JObject, JString, JValue};
use jni::sys::jobject;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct Album {
    artist: String,
    cover: Vec<u8>,
    name: String,
    tracks: Vec<Track>,
    year: u32,
    genre: String,
}

impl Album {
    pub fn rust_album_to_java(&self, env: &mut JNIEnv) -> jobject {
        // Find Album class
        let album_class = env
            .find_class("com/kgaft/VoidAudioPlayer/Verbose/Album")
            .unwrap();

        // Create new Album object
        let album_obj = env.alloc_object(album_class).unwrap();

        // Set artist
        let j_artist = env.new_string(&self.artist).unwrap();
        env.call_method(
            album_obj.as_ref(),
            "setArtist",
            "(Ljava/lang/String;)V",
            &[JValue::Object(&j_artist.into())],
        )
        .unwrap();

        // Set name
        let j_name = env.new_string(&self.name).unwrap();
        env.call_method(
            album_obj.as_ref(),
            "setName",
            "(Ljava/lang/String;)V",
            &[JValue::Object(&j_name.into())],
        )
        .unwrap();

        // Set year
        env.call_method(
            album_obj.as_ref(),
            "setYear",
            "(I)V",
            &[JValue::Int(self.year as i32)],
        )
        .unwrap();

        // Set genre
        let j_genre = env.new_string(&self.genre).unwrap();
        env.call_method(
            album_obj.as_ref(),
            "setGenre",
            "(Ljava/lang/String;)V",
            &[JValue::Object(&j_genre.into())],
        )
        .unwrap();

        // Handle tracks: convert Vec<Track> → java.util.ArrayList<Track>
        let arraylist_class = env.find_class("java/util/ArrayList").unwrap();
        let j_tracks = env.new_object(arraylist_class, "()V", &[]).unwrap();

        unsafe {
            for track in &self.tracks {
                let j_track = track.to_jobject(env).unwrap();
                let track_obj = JObject::from_raw(j_track);
                env.call_method(
                    j_tracks.as_ref(),
                    "add",
                    "(Ljava/lang/Object;)Z",
                    &[JValue::Object(&track_obj)],
                )
                .unwrap();
            }
        }

        env.call_method(
            album_obj.as_ref(),
            "setTracks",
            "(Ljava/util/List;)V",
            &[JValue::Object(&j_tracks)],
        )
        .unwrap();

        // Set cover (byte[])
        let j_cover: JByteArray = env.byte_array_from_slice(&self.cover).unwrap();
        env.call_method(
            album_obj.as_ref(),
            "setCover",
            "([B)V",
            &[JValue::Object(&j_cover.into())],
        )
        .unwrap();

        album_obj.as_raw()
    }

    pub fn java_album_to_rust(env: &mut JNIEnv, album_obj: JObject) -> Album {
        // Artist
        let j_artist: JString = env
            .call_method(album_obj.as_ref(), "getArtist", "()Ljava/lang/String;", &[])
            .unwrap()
            .l()
            .unwrap()
            .into();
        let artist: String = env.get_string(&j_artist).unwrap().into();

        // Cover
        let j_cover: JByteArray = env
            .call_method(album_obj.as_ref(), "getCover", "()[B", &[])
            .unwrap()
            .l()
            .unwrap()
            .into();
        let cover = if !j_cover.as_raw().is_null() {
            env.convert_byte_array(j_cover).unwrap()
        } else {
            Vec::new()
        };

        // Name
        let j_name: JString = env
            .call_method(album_obj.as_ref(), "getName", "()Ljava/lang/String;", &[])
            .unwrap()
            .l()
            .unwrap()
            .into();
        let name: String = env.get_string(&j_name).unwrap().into();

        // Year
        let year = env
            .call_method(album_obj.as_ref(), "getYear", "()I", &[])
            .unwrap()
            .i()
            .unwrap() as u32;

        // Genre
        let j_genre: JString = env
            .call_method(album_obj.as_ref(), "getGenre", "()Ljava/lang/String;", &[])
            .unwrap()
            .l()
            .unwrap()
            .into();
        let genre: String = env.get_string(&j_genre).unwrap().into();

        // Tracks
        let j_tracks = env
            .call_method(album_obj.as_ref(), "getTracks", "()Ljava/util/List;", &[])
            .unwrap()
            .l()
            .unwrap();

        let size = env
            .call_method(j_tracks.as_ref(), "size", "()I", &[])
            .unwrap()
            .i()
            .unwrap();

        let mut tracks = Vec::new();
        for i in 0..size {
            let j_track = env
                .call_method(
                    j_tracks.as_ref(),
                    "get",
                    "(I)Ljava/lang/Object;",
                    &[JValue::Int(i)],
                )
                .unwrap()
                .l()
                .unwrap();
            let track = Track::from_jobject(env, j_track).unwrap(); // assume you implement this
            tracks.push(track);
        }

        Album {
            artist,
            cover,
            name,
            tracks,
            year,
            genre,
        }
    }
}

#[derive(Serialize, Deserialize)]
pub struct Library {
    pub albums: Vec<Album>,
}
