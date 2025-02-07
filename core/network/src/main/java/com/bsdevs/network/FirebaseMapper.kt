package com.bsdevs.network

interface FirebaseMapper<in MAP, out DTO> {
    fun mapToDto(map: MAP): DTO
}

interface DataMapper<in Dto, out Data> {
    fun mapToData(dto: Dto): Data
}