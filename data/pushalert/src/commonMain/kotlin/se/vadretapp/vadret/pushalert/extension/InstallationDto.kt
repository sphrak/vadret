package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.InstallationDto
import se.vadretapp.vadret.pushalert.model.InstallationToken

internal fun InstallationDto.toInstallationToken(): InstallationToken =
    InstallationToken(value = checkNotNull(value = installationToken) { "The server installed without an installation token" })