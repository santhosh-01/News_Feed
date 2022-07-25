package com.example.newsfeed.util

class Constants {

    companion object {
        const val BASE_URL = "https://newsapi.org"
        val API_KEYS: List<String> = listOf(
            "a306481674124290aeb489fc6d9b3566",
            "eebedaf274924cecb6d828149b79680e",
            "4851d2859ad9447ca79ab81951d3c4f6",
            "6328ad2ed8d346d9b9787cc5926e8733",
            "4918214f38ac4a48a7aa67b43e0104af",
            "168ed7f5e74447b4820bdd5261250421",
            "7fed03f410bd4efe854b3a2dd8cc4463",
        )

        //        santhosh.110999
//        const val API_KEY = "eebedaf274924cecb6d828149b79680e"
//        const val API_KEY = "4851d2859ad9447ca79ab81951d3c4f6"
//        const val API_KEY = "6328ad2ed8d346d9b9787cc5926e8733"
//        const val API_KEY = "4918214f38ac4a48a7aa67b43e0104af" Marnish
//        168ed7f5e74447b4820bdd5261250421 9000kj
//        7fed03f410bd4efe854b3a2dd8cc4463 50kj
        //        a306481674124290aeb489fc6d9b3566 Karthick
        const val SEARCH_NEWS_TIME_DELAY = 500L
        const val QUERY_PAGE_SIZE = 10
        const val TOTAL_RECORD = 100
    }

}