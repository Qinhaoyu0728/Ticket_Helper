package com.example.tickethelper.model

// 国内久事APP渠道

// 存储单组showId和sign
data class ShowConfig(
    val name: String,
    val showId: String,
    val sign: String
)

// 内置
object ShowConfigRepository {
    val defaultConfigs = listOf(
        ShowConfig(
            name = "A看台",
            showId = "6931332204da960001241231",
            sign = "RTJGQTY1QkY5N0Q0NUM1MzM4RkEwNDk0Q0I5MjcxMTk="
        ),
        ShowConfig(
            name = "B看台",
            showId = "693132e14996310001244821",
            sign = "N0M1MDBBMTBDQkNCOEM4Njk1REVBMDVDNTU4ODQxMTM="
        ),
        ShowConfig(
            name = "H看台",
            showId = "69315292499631000125952f",
            sign = "MzBCMTNDRjdFNTBDQjdDOTEwREExM0NGQTZCM0MwMzc="
        ),
        ShowConfig(
            name = "K看台",
            showId = "693152ad4996310001259691",
            sign = "RUI0N0ZDNUU1QjgzNzBEMTY3QzQyRUM5QjQ4QkMyMzk="
        ),
        ShowConfig(
            name = "E看台",
            showId = "693152c604da960001255ee6",
            sign = "MUU2NkIxNkNGNjA5NDg4NTg2RENFQTJEQjE3NjRBMzY="
        ),
        ShowConfig(
            name = "C/F/J草地看台",
            showId = "6931535304da960001256176",
            sign = "NjVDMTNFRDY2MTU0NUU0QUUzQTlGM0U3MTZDMzlCOTg="
        ),
        ShowConfig(
            name = "F1×迪士尼限量联名套票",
            showId = "6932f256499631000135691a",
            sign = "RTUwMTJEOUI3MTRGOUI0NzEwNzRBQzI4ODFBMzQzQTg="
        ),
        ShowConfig(
            name = "铂金体验之旅",
            showId = "693147634996310001251e16",
            sign = "NzE0QkI3MEQ3MEIzRkY5MzZGREVGMTQwQzYwRjIxQjE="
        ),
        ShowConfig(
            name = "A铂金看台",
            showId = "6931340149963100012455d5",
            sign = "QTU4RDhGQzkwQUVFNUYzRDMyMzc5MjQ1NTQ3NjNDM0I="
        ),
        ShowConfig(
            name = "E看台车迷应援区",
            showId = "6931533e49963100012599e9",
            sign = "NkVEMUMwOURCNzBGODM0NDNCOTVGNDgzQTE3RkE2OTc="
        )
    )
}