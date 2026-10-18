package com.example.data

object LocalMusicCatalog {

    val genres = listOf(
        "All Scenes",
        "Indie Rock",
        "Lo-Fi Beats",
        "Neo-Soul",
        "Synthwave",
        "Folk Acoustic",
        "Underground Hip-Hop"
    )

    val cities = listOf(
        "All Cities",
        "Los Angeles, CA",
        "Brooklyn, NY",
        "Austin, TX",
        "Seattle, WA",
        "Chicago, IL",
        "Atlanta, GA"
    )

    val curatedTracks: List<ArtistTrack> = listOf(
        ArtistTrack(
            id = "curated_velvet_1",
            uploaderId = "sonic_editorial",
            artistName = "The Velvet Echoes",
            trackTitle = "Sunset Boulevard Drift",
            city = "Los Angeles, CA",
            genre = "Indie Rock",
            streamUrl = "soniclocal://synth/indie_sunset",
            bio = "Four-piece garage dream-pop band born in Silver Lake basement sessions.",
            durationSec = 194L,
            isPublic = true,
            playsCount = 1420L
        ),
        ArtistTrack(
            id = "curated_velvet_2",
            uploaderId = "sonic_editorial",
            artistName = "The Velvet Echoes",
            trackTitle = "Echo Park Neon Rain",
            city = "Los Angeles, CA",
            genre = "Indie Rock",
            streamUrl = "soniclocal://synth/indie_rain",
            bio = "Four-piece garage dream-pop band born in Silver Lake basement sessions.",
            durationSec = 212L,
            isPublic = true,
            playsCount = 980L
        ),
        ArtistTrack(
            id = "curated_kaia_1",
            uploaderId = "sonic_editorial",
            artistName = "Kaia Sol & The Brass Collective",
            trackTitle = "Bed-Stuy Golden Hour",
            city = "Brooklyn, NY",
            genre = "Neo-Soul",
            streamUrl = "soniclocal://synth/neosoul_golden",
            bio = "Warm Rhodes keys, upright bass, and rooftop horn arrangements from Brooklyn.",
            durationSec = 228L,
            isPublic = true,
            playsCount = 2150L
        ),
        ArtistTrack(
            id = "curated_kaia_2",
            uploaderId = "sonic_editorial",
            artistName = "Kaia Sol & The Brass Collective",
            trackTitle = "Velvet Bodega Groove",
            city = "Brooklyn, NY",
            genre = "Neo-Soul",
            streamUrl = "soniclocal://synth/neosoul_bodega",
            bio = "Warm Rhodes keys, upright bass, and rooftop horn arrangements from Brooklyn.",
            durationSec = 186L,
            isPublic = true,
            playsCount = 1640L
        ),
        ArtistTrack(
            id = "curated_kuro_1",
            uploaderId = "sonic_editorial",
            artistName = "Kuro Tape Deck",
            trackTitle = "Rain on Capitol Hill",
            city = "Seattle, WA",
            genre = "Lo-Fi Beats",
            streamUrl = "soniclocal://synth/lofi_rain",
            bio = "Analog cassette beatmaker sampling espresso machines and Pacific Northwest rain.",
            durationSec = 165L,
            isPublic = true,
            playsCount = 3190L
        ),
        ArtistTrack(
            id = "curated_kuro_2",
            uploaderId = "sonic_editorial",
            artistName = "Kuro Tape Deck",
            trackTitle = "Ferry Terminal 2AM",
            city = "Seattle, WA",
            genre = "Lo-Fi Beats",
            streamUrl = "soniclocal://synth/lofi_ferry",
            bio = "Analog cassette beatmaker sampling espresso machines and Pacific Northwest rain.",
            durationSec = 178L,
            isPublic = true,
            playsCount = 1890L
        ),
        ArtistTrack(
            id = "curated_mirage_1",
            uploaderId = "sonic_editorial",
            artistName = "Red River Mirage",
            trackTitle = "East Austin Analog Horizon",
            city = "Austin, TX",
            genre = "Synthwave",
            streamUrl = "soniclocal://synth/synthwave_horizon",
            bio = "Hardware modular synth duo performing live arpeggio sets in East Austin patios.",
            durationSec = 205L,
            isPublic = true,
            playsCount = 1750L
        ),
        ArtistTrack(
            id = "curated_clara_1",
            uploaderId = "sonic_editorial",
            artistName = "Clara Vance",
            trackTitle = "Lake Michigan Pines",
            city = "Chicago, IL",
            genre = "Folk Acoustic",
            streamUrl = "soniclocal://synth/folk_pines",
            bio = "Fingerstyle guitarist and lyricist weaving intimate Midwest storytelling.",
            durationSec = 200L,
            isPublic = true,
            playsCount = 1230L
        ),
        ArtistTrack(
            id = "curated_cipher_1",
            uploaderId = "sonic_editorial",
            artistName = "Southside Cipher Crew",
            trackTitle = "BeltLine Midnight Transit",
            city = "Atlanta, GA",
            genre = "Underground Hip-Hop",
            streamUrl = "soniclocal://synth/hiphop_transit",
            bio = "Live jazz-rap collective blending MPC boom-bap with soulful Rhodes chords.",
            durationSec = 190L,
            isPublic = true,
            playsCount = 2540L
        )
    )

    val artistSpotlights: List<LocalArtistSpotlight> = listOf(
        LocalArtistSpotlight(
            id = "artist_velvet",
            artistName = "The Velvet Echoes",
            city = "Los Angeles, CA",
            neighborhood = "Silver Lake",
            primaryGenre = "Indie Rock",
            bio = "Known for shimmering 12-string Rickenbacker riffs and tape-saturated vocals recorded in a converted Echo Park garage.",
            monthlyListeners = 14800,
            nextVenue = "Fri 9PM • The Satellite Stage, Silver Lake",
            accentHex = 0xFFFFB300,
            tracks = curatedTracks.filter { it.artistName == "The Velvet Echoes" }
        ),
        LocalArtistSpotlight(
            id = "artist_kaia",
            artistName = "Kaia Sol & The Brass Collective",
            city = "Brooklyn, NY",
            neighborhood = "Bed-Stuy / Williamsburg",
            primaryGenre = "Neo-Soul",
            bio = "Seven-piece collective bringing late-night rooftop jam sessions and lush analog soul harmonies to Brooklyn.",
            monthlyListeners = 22400,
            nextVenue = "Sat 10PM • Baby's All Right, Brooklyn",
            accentHex = 0xFFFF5252,
            tracks = curatedTracks.filter { it.artistName == "Kaia Sol & The Brass Collective" }
        ),
        LocalArtistSpotlight(
            id = "artist_kuro",
            artistName = "Kuro Tape Deck",
            city = "Seattle, WA",
            neighborhood = "Capitol Hill",
            primaryGenre = "Lo-Fi Beats",
            bio = "Produces warm SP-404 tape loops recorded live at independent coffee roasters and record shops across Seattle.",
            monthlyListeners = 31200,
            nextVenue = "Thu 8PM • Neumos Loft, Capitol Hill",
            accentHex = 0xFF00E676,
            tracks = curatedTracks.filter { it.artistName == "Kuro Tape Deck" }
        ),
        LocalArtistSpotlight(
            id = "artist_mirage",
            artistName = "Red River Mirage",
            city = "Austin, TX",
            neighborhood = "East Cesar Chavez",
            primaryGenre = "Synthwave",
            bio = "Vintage Juno-106 and Prophet-6 synthesizers meet driving nocturnal basslines from the heart of Austin.",
            monthlyListeners = 18600,
            nextVenue = "Sun 9PM • Cheer Up Charlies, Austin",
            accentHex = 0xFFB388FF,
            tracks = curatedTracks.filter { it.artistName == "Red River Mirage" }
        ),
        LocalArtistSpotlight(
            id = "artist_clara",
            artistName = "Clara Vance",
            city = "Chicago, IL",
            neighborhood = "Logan Square",
            primaryGenre = "Folk Acoustic",
            bio = "Warm acoustic harmonies and cello arrangements captured in intimate Logan Square listening rooms.",
            monthlyListeners = 11900,
            nextVenue = "Wed 8PM • Schubas Tavern, Chicago",
            accentHex = 0xFF18FFFF,
            tracks = curatedTracks.filter { it.artistName == "Clara Vance" }
        ),
        LocalArtistSpotlight(
            id = "artist_cipher",
            artistName = "Southside Cipher Crew",
            city = "Atlanta, GA",
            neighborhood = "Little Five Points",
            primaryGenre = "Underground Hip-Hop",
            bio = "Improvised live-instrument hip-hop cyphers championing local lyricists and brass players.",
            monthlyListeners = 26900,
            nextVenue = "Fri 11PM • Aisle 5, Little Five Points",
            accentHex = 0xFFFFB300,
            tracks = curatedTracks.filter { it.artistName == "Southside Cipher Crew" }
        )
    )

    val featuredCommunityPlaylists: List<Playlist> = listOf(
        Playlist(
            id = "featured_rooftop_ny",
            ownerId = "curator_maya",
            ownerName = "Maya Lin (Brooklyn Curator)",
            title = "Williamsburg Rooftop Golden Hour",
            description = "Warm neo-soul, lo-fi tape beats, and indie grooves for sunset hangs with friends.",
            neighborhood = "Williamsburg, NYC",
            moodTag = "Rooftop Grooves",
            coverTheme = "crimson",
            isShared = true,
            shareCode = "SONIC-NYC1",
            trackIds = listOf("curated_kaia_1", "curated_kaia_2", "curated_kuro_1", "curated_cipher_1"),
            likesCount = 84L
        ),
        Playlist(
            id = "featured_eastside_la",
            ownerId = "curator_devon",
            ownerName = "Devon Reyes (Echo Park Zine)",
            title = "Silver Lake Basement Tapes",
            description = "Handpicked local bands playing shows this weekend across East LA.",
            neighborhood = "Silver Lake, LA",
            moodTag = "Late Night Indie",
            coverTheme = "amber",
            isShared = true,
            shareCode = "SONIC-LA88",
            trackIds = listOf("curated_velvet_1", "curated_velvet_2", "curated_mirage_1", "curated_clara_1"),
            likesCount = 62L
        ),
        Playlist(
            id = "featured_rainy_pnw",
            ownerId = "curator_sora",
            ownerName = "Sora Takahashi (Capitol Hill Vinyl)",
            title = "Espresso & Analog Rain Loops",
            description = "Cozy instrumental beats and acoustic folk from independent local studios.",
            neighborhood = "Capitol Hill, Seattle",
            moodTag = "Coffeehouse Study",
            coverTheme = "emerald",
            isShared = true,
            shareCode = "SONIC-SEA9",
            trackIds = listOf("curated_kuro_1", "curated_kuro_2", "curated_clara_1"),
            likesCount = 119L
        )
    )
}
