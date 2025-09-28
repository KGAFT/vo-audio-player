use dsf::DsfFile;
use id3::{Tag, TagLike};
use jni::JNIEnv;
use jni::objects::{JObject, JValue};
use jni::sys::{jbyte, jint, jobject};
use lofty::file::{AudioFile, TaggedFileExt};
use lofty::tag::Accessor;
use serde::{Deserialize, Serialize};
use std::borrow::Cow;
use std::fs::File;
use std::io::BufReader;
use std::path::{Path, PathBuf};

#[derive(Serialize, Deserialize)]
pub struct Track {
    name: String,
    album_name: String,
    artis_name: String,
    genre: String,
    path: String,
    year: u32,
    bit_depth: u8,
    sample_rate: u32,
    channels: u8,
    overall_bitrate: u32,
    cover_bytes: Vec<u8>,
    offset_ms: u64,
    duration_ms: u64,
}

impl Track {
    pub fn new(path: &str, env: &mut JNIEnv) -> jobject {
        // Read track metadata using lofty
        let path_buf = PathBuf::from(path);
        let f_name = path_buf.file_name().unwrap().to_str().unwrap().to_string();
        if f_name.ends_with(".dsf") || f_name.ends_with(".dff") || f_name.ends_with(".dsd") {
            return Self::extract_dsd_info(path, env);
        }
        let tagged_file = lofty::read_from_path(path);
        if tagged_file.is_err() {
            println!("Failed to read track info: {}", path);
            return Self::extract_dsd_info(path, env);
        }
        let tagged_file = tagged_file.unwrap();

        let properties = tagged_file.properties();
        let info = tagged_file.primary_tag();
        if info.is_none() {
            return Self::empty_track(path, env);
        }
        let info = info.unwrap();
        // Create Java strings
        let album_name = env
            .new_string(info.album().unwrap_or(Cow::from("")))
            .unwrap();
        let artist_name = env
            .new_string(info.artist().unwrap_or(Cow::from("")))
            .unwrap();
        let track_name = if let Some(title) = info.title() {
            env.new_string(title.as_ref()).unwrap()
        } else {
            env.new_string(f_name.clone()).unwrap()
        };
        let genre_name = env
            .new_string(
                info.genre()
                    .unwrap_or(PathBuf::from(path).file_name().unwrap().to_string_lossy()),
            )
            .unwrap();
        let path_str = env.new_string(path).unwrap();

        // Get primitive values
        let year: jint = info.year().unwrap_or(0) as jint;
        let bit_depth: jbyte = properties.bit_depth().unwrap_or(0) as jbyte;
        let sample_rate: jint = properties.sample_rate().unwrap_or(0) as jint;
        let channels: jbyte = properties.channels().unwrap_or(0) as jbyte;
        let overall_bitrate: jint = properties.overall_bitrate().unwrap_or(0) as jint;

        // Find Track class and constructor
        let track_class = env
            .find_class("com/kgaft/VoidAudioPlayer/Native/Track")
            .expect("Failed to find Track class");

        // Create Track object
        let track_obj = env
            .new_object(track_class, "()V", &[])
            .expect("Failed to create Track object");

        // Set String fields
        let string_fields = [
            ("name", &track_name),
            ("albumName", &album_name),
            ("artistName", &artist_name),
            ("genre", &genre_name),
            ("path", &path_str),
        ];

        for (field_name, value) in string_fields.iter() {
            env.set_field(
                track_obj.as_ref(),
                *field_name,
                "Ljava/lang/String;",
                JValue::from(value),
            )
            .expect(&format!("Failed to set field {}", field_name));
        }

        // Set int fields
        let int_fields = [
            ("year", year),
            ("sampleRate", sample_rate),
            ("overallBitrate", overall_bitrate),
        ];

        for (field_name, value) in int_fields.iter() {
            env.set_field(track_obj.as_ref(), *field_name, "I", JValue::Int(*value))
                .expect(&format!("Failed to set field {}", field_name));
        }

        // Set byte fields
        let byte_fields = [("bitDepth", bit_depth), ("channels", channels)];

        for (field_name, value) in byte_fields.iter() {
            env.set_field(track_obj.as_ref(), *field_name, "B", JValue::Byte(*value))
                .expect(&format!("Failed to set field {}", field_name));
        }
        if info.picture_count() > 0 {
            let picture_bytes = env
                .byte_array_from_slice(info.pictures()[0].data())
                .expect("Failed to parse bytes image");
            env.set_field(
                track_obj.as_ref(),
                "pictureBytes",
                "[B",
                JValue::from(&picture_bytes),
            )
            .expect("Failed to set image bytes");
        }

        track_obj.as_raw()
    }

    pub fn to_jobject(&self, env: &mut JNIEnv) -> jni::errors::Result<jobject> {
        // Find Java class
        let track_class = env.find_class("com/kgaft/VoidAudioPlayer/Native/Track")?;

        // Create empty Track object via no-arg constructor
        let track_obj = env.new_object(track_class, "()V", &[])?;
        let track_obj2 = track_obj.as_ref();
        // Helper to set string fields
        let mut set_string_field = |name: &str, value: &str| -> jni::errors::Result<()> {
            let jstr = env.new_string(value)?;
            env.set_field(
                track_obj2,
                name,
                "Ljava/lang/String;",
                JValue::Object(jstr.as_ref()),
            )?;
            Ok(())
        };

        // Set String fields
        set_string_field("name", &self.name)?;
        set_string_field("albumName", &self.album_name)?;
        set_string_field("artistName", &self.artis_name)?;
        set_string_field("genre", &self.genre)?;
        set_string_field("path", &self.path)?;

        // Set int fields
        env.set_field(
            track_obj.as_ref(),
            "year",
            "I",
            JValue::Int(self.year as i32),
        )?;
        env.set_field(
            track_obj.as_ref(),
            "sampleRate",
            "I",
            JValue::Int(self.sample_rate as i32),
        )?;
        env.set_field(
            track_obj.as_ref(),
            "overallBitrate",
            "I",
            JValue::Int(self.overall_bitrate as i32),
        )?;

        // Set byte fields
        env.set_field(
            track_obj.as_ref(),
            "bitDepth",
            "B",
            JValue::Byte(self.bit_depth as i8),
        )?;
        env.set_field(
            track_obj.as_ref(),
            "channels",
            "B",
            JValue::Byte(self.channels as i8),
        )?;

        // Set long fields
        env.set_field(
            track_obj.as_ref(),
            "offsetMs",
            "J",
            JValue::Long(self.offset_ms as i64),
        )?;
        env.set_field(
            track_obj.as_ref(),
            "durationMs",
            "J",
            JValue::Long(self.duration_ms as i64),
        )?;

        // Set picture bytes
        if !self.cover_bytes.is_empty() {
            let byte_array = env.byte_array_from_slice(&self.cover_bytes)?;
            env.set_field(
                track_obj.as_ref(),
                "pictureBytes",
                "[B",
                JValue::Object(&JObject::from(byte_array)),
            )?;
        }

        Ok(track_obj.as_raw())
    }

    pub fn from_jobject(env: &mut JNIEnv, track_obj: JObject) -> jni::errors::Result<Self> {
        // Helper closure to get a Java String field and convert to Rust String
        let track_obj2 = track_obj.as_ref();
        let mut env_copy = unsafe { env.unsafe_clone() };
        let mut get_string_field = |name: &str| -> jni::errors::Result<String> {
            unsafe {
                let jstr = env_copy
                    .get_field(track_obj2, name, "Ljava/lang/String;")?
                    .l()?;
                if jstr.is_null() {
                    Ok(String::new())
                } else {
                    Ok(env_copy
                        .get_string(&jni::objects::JString::from(jstr))?
                        .into())
                }
            }
        };
        let mut env_copy2 = unsafe { env.unsafe_clone() };
        // Helper closure to get a byte array field
        let mut get_byte_array_field = |name: &str| -> jni::errors::Result<Vec<u8>> {
            let jarr = env_copy2.get_field(track_obj2, name, "[B")?.l()?;
            if jarr.is_null() {
                Ok(Vec::new())
            } else {
                Ok(env_copy2.convert_byte_array(jni::objects::JByteArray::from(jarr))?)
            }
        };

        Ok(Track {
            name: get_string_field("name")?,
            album_name: get_string_field("albumName")?,
            artis_name: get_string_field("artistName")?,
            genre: get_string_field("genre")?,
            path: get_string_field("path")?,

            year: u32::try_from(env.get_field(track_obj.as_ref(), "year", "I")?.i()?).unwrap_or(0),
            bit_depth: u8::try_from(env.get_field(track_obj.as_ref(), "bitDepth", "B")?.b()?)
                .unwrap_or(0),
            sample_rate: u32::try_from(env.get_field(track_obj.as_ref(), "sampleRate", "I")?.i()?)
                .unwrap_or(0),
            channels: u8::try_from(env.get_field(track_obj.as_ref(), "channels", "B")?.b()?)
                .unwrap_or(0),
            overall_bitrate: u32::try_from(
                env.get_field(track_obj.as_ref(), "overallBitrate", "I")?
                    .i()?,
            )
            .unwrap_or(0),

            cover_bytes: get_byte_array_field("pictureBytes")?,

            offset_ms: u64::try_from(env.get_field(track_obj.as_ref(), "offsetMs", "J")?.j()?)
                .unwrap_or(0),
            duration_ms: u64::try_from(env.get_field(track_obj.as_ref(), "durationMs", "J")?.j()?)
                .unwrap_or(0),
        })
    }

    pub fn extract_dsd_info(path: &str, env: &mut JNIEnv) -> jobject {
        let f_name = PathBuf::from(path)
            .file_name()
            .unwrap()
            .to_str()
            .unwrap()
            .to_string();

        /*
        // .dff or .dsd files → no metadata support
        if f_name.ends_with(".dff") || f_name.ends_with(".dsd") {
            return Self::empty_track(path, env);
        }

         */

        // Parse DSF file
        let dsf = match DsfFile::open(Path::new(path)) {
            Ok(f) => f,
            Err(_) => return Self::empty_track(path, env),
        };

        // Require an ID3 tag
        let tag: &Tag = match dsf.id3_tag() {
            Some(t) => t,
            None => return Self::empty_track(path, env),
        };

        let fmt = dsf.fmt_chunk();

        let path_buf = PathBuf::from(path);
        let f_name = path_buf.file_name().unwrap().to_str().unwrap().to_string();
        // Strings from id3::Tag
        let album_name = env.new_string(tag.album().unwrap_or("")).unwrap();
        let artist_name = env.new_string(tag.artist().unwrap_or("")).unwrap();
        let track_name = env
            .new_string(tag.title().unwrap_or(f_name.as_str()))
            .unwrap();
        let genre_name = env.new_string(tag.genre().unwrap_or("")).unwrap();
        let path_str = env.new_string(path).unwrap();

        // Numeric properties
        let sample_rate: jint = fmt.sampling_frequency() as jint;
        let channels: jbyte = fmt.channel_num() as jbyte;
        let bit_depth: jbyte = fmt.bits_per_sample() as jbyte; // usually 1
        let year: jint = tag.year().unwrap_or(0) as jint;

        // Approximate bitrate
        let overall_bitrate: jint =
            (sample_rate as i64 * channels as i64 * bit_depth as i64) as jint;

        // Create Track class
        let track_class = env
            .find_class("com/kgaft/VoidAudioPlayer/Native/Track")
            .expect("Failed to find Track class");
        let track_obj = env
            .new_object(track_class, "()V", &[])
            .expect("Failed to create Track object");

        // Set String fields
        let string_fields = [
            ("name", &track_name),
            ("albumName", &album_name),
            ("artistName", &artist_name),
            ("genre", &genre_name),
            ("path", &path_str),
        ];
        for (field_name, value) in string_fields.iter() {
            env.set_field(
                track_obj.as_ref(),
                *field_name,
                "Ljava/lang/String;",
                JValue::from(value),
            )
            .unwrap();
        }

        // Set int fields
        let int_fields = [
            ("year", year),
            ("sampleRate", sample_rate),
            ("overallBitrate", overall_bitrate),
        ];
        for (field_name, value) in int_fields.iter() {
            env.set_field(track_obj.as_ref(), *field_name, "I", JValue::Int(*value))
                .unwrap();
        }

        // Set byte fields
        let byte_fields = [("bitDepth", bit_depth), ("channels", channels)];
        for (field_name, value) in byte_fields.iter() {
            env.set_field(track_obj.as_ref(), *field_name, "B", JValue::Byte(*value))
                .unwrap();
        }

        for pic in tag.pictures() {
            let picture_bytes = env
                .byte_array_from_slice(&pic.data)
                .expect("Failed to create byte array for picture");
            env.set_field(
                track_obj.as_ref(),
                "pictureBytes",
                "[B",
                JValue::from(&picture_bytes),
            )
            .expect("Failed to set image bytes");
            break;
        }
        // Picture (cover art) if available
        track_obj.as_raw()
    }
    fn empty_track(path: &str, env: &mut JNIEnv) -> jobject {
        let track_class = env
            .find_class("com/kgaft/VoidAudioPlayer/Native/Track")
            .expect("Failed to find Track class");
        let track_obj = env
            .new_object(track_class, "()V", &[])
            .expect("Failed to create Track object");

        let empty_str = env.new_string("").unwrap();
        let path_str = env.new_string(path).unwrap();

        // Fill string fields with empty/path
        let string_fields = [
            ("name", &empty_str),
            ("albumName", &empty_str),
            ("artistName", &empty_str),
            ("genre", &empty_str),
            ("path", &path_str),
        ];
        for (field_name, value) in string_fields.iter() {
            env.set_field(
                track_obj.as_ref(),
                *field_name,
                "Ljava/lang/String;",
                JValue::from(value),
            )
            .unwrap();
        }

        // Fill numbers with 0
        env.set_field(track_obj.as_ref(), "year", "I", JValue::Int(0))
            .unwrap();
        env.set_field(track_obj.as_ref(), "sampleRate", "I", JValue::Int(0))
            .unwrap();
        env.set_field(track_obj.as_ref(), "overallBitrate", "I", JValue::Int(0))
            .unwrap();
        env.set_field(track_obj.as_ref(), "bitDepth", "B", JValue::Byte(0))
            .unwrap();
        env.set_field(track_obj.as_ref(), "channels", "B", JValue::Byte(0))
            .unwrap();

        track_obj.as_raw()
    }
}
