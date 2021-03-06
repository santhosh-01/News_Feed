package com.example.newsfeed.util

class Constants {

    companion object {
        const val BASE_URL = "https://newsapi.org"
        val API_KEYS: List<String> = listOf(
            "eebedaf274924cecb6d828149b79680e",
            "4851d2859ad9447ca79ab81951d3c4f6",
            "6328ad2ed8d346d9b9787cc5926e8733",
            "4918214f38ac4a48a7aa67b43e0104af",
            "168ed7f5e74447b4820bdd5261250421",
            "7fed03f410bd4efe854b3a2dd8cc4463",
            "a306481674124290aeb489fc6d9b3566",
            "253ee6dbb0f14f6393b73293d2b11325",
            "9bae052cac444a6dbf6c0ca6e33c71a6",
            "346d34cc24164d09bfefc85e575a198b",
            "823b44e9573e44c593a63623e232164e"
        )

        // const val API_KEY = "eebedaf274924cecb6d828149b79680e"
        // const val API_KEY = "4851d2859ad9447ca79ab81951d3c4f6"
        // const val API_KEY = "6328ad2ed8d346d9b9787cc5926e8733"
        // const val API_KEY = "4918214f38ac4a48a7aa67b43e0104af"
        // 168ed7f5e74447b4820bdd5261250421
        // 7fed03f410bd4efe854b3a2dd8cc4463
        // a306481674124290aeb489fc6d9b3566
        // "253ee6dbb0f14f6393b73293d2b11325"
        // "9bae052cac444a6dbf6c0ca6e33c71a6"
        // "346d34cc24164d09bfefc85e575a198b"
        // "823b44e9573e44c593a63623e232164e"
        const val SEARCH_NEWS_TIME_DELAY = 500L
        const val QUERY_PAGE_SIZE = 10
        const val TOTAL_RECORD = 100
    }

}