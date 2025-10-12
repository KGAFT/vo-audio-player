use std::time::Duration;

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
    let cue = rcue::parser::parse_from_file(path, false);
    if cue.is_err(){
        eprintln!("{}", cue.unwrap_err().to_string());
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