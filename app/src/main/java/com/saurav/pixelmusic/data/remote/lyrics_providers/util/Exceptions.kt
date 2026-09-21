package com.saurav.pixelmusic.data.remote.lyrics_providers.util

class NoTrackFoundException : Exception("No track found on the provider.")
class EmptyQueryException : Exception("The search query was empty.")
class InternalErrorException(message: String) : Exception(message)

