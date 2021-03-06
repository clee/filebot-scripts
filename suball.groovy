// filebot -script fn:suball /path/to/media -non-strict --def maxAgeDays=7


def minAgeTimeStamp = tryQuietly{ now.time - (minAgeDays.toDouble() * 24 * 60 * 60 * 1000).toLong() }
def maxAgeTimeStamp = tryQuietly{ now.time - (maxAgeDays.toDouble() * 24 * 60 * 60 * 1000).toLong() }
def minFileSize = tryQuietly{ minFileSize.toLong() }; if (minFileSize == null) { minFileSize = 50 * 1000L * 1000L }
def minLengthMS = tryQuietly{ minLengthMS.toLong() }; if (minLengthMS == null) { minLengthMS = 10 * 60 * 1000L }
def ignore = tryQuietly{ ignore } ?: null


def accept = { f ->
	// ignore files that match the give ignore pattern
	if (f.path =~ ignore)
		return false

	// ignore files that are too young
	if (minAgeTimeStamp != null && f.creationDate > minAgeTimeStamp)
		return false

	// ignore files that are too old
	if (maxAgeTimeStamp != null && f.creationDate < maxAgeTimeStamp)
		return false
	
	// ignore files that are too small	
	if (minFileSize > 0 && f.length() < minFileSize)
		return false
	
	// ignore files that are too short
	if ((minLengthMS > 0 && tryQuietly{ getMediaInfo(file:f, format:'{duration}').toLong() < minLengthMS }))
		return false
	
	// ignore files that already have subtitles
	if (getMediaInfo(file:f, format:'''{media.TextCodecList}''').length() > 0)
		return false
	
	return true
}


/*
 * Get subtitles for all your media files  
 */
args.eachMediaFolder { dir ->
	def input = dir.listFiles{ f -> f.isVideo() }
	def selected = input.findAll{ f -> accept(f) }
		
	if (selected.size() > 0) {
		log.info "Fetch subtitles for [$dir]"
		
		// print excludes
		(input - selected).each{ f -> log.finest "Exclude: $f" }

		getMissingSubtitles(file: selected)
	} else {
		log.finest "Exclude: $dir"
	}
}
