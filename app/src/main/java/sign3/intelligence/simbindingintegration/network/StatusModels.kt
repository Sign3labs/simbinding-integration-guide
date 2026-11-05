package sign3.intelligence.simbindingintegration.network

data class StatusResponse(
    val auths: List<Auth>? = null,
    val derivedOperator: String? = null
) {
    data class Auth(
        val identityValue: String? = null,
        val status: String? = null,
        val error: Error? = null
    )

    data class Error(
        val errorCode: String? = null,
        val message: String? = null,
        val description: String? = null
    )
}

/** Body of 4xx / 5xx responses, e.g. {"message":"Invalid Request","errorCode":"7119","description":"..."} */
data class ApiErrorResponse(
    val message: String? = null,
    val errorCode: String? = null,
    val description: String? = null
)
