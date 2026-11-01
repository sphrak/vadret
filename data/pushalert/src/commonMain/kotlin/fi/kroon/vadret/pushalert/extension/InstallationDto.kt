package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.InstallationDto
import fi.kroon.vadret.pushalert.model.InstallationToken

internal fun InstallationDto.toInstallationToken(): InstallationToken =
    InstallationToken(value = checkNotNull(value = installationToken) { "The server installed without an installation token" })