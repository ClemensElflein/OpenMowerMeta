package de.openmower.backend.endpoints

import de.openmower.backend.SettingDTO
import de.openmower.backend.api.SettingApiService
import org.springframework.stereotype.Service

@Service
class SettingApiServiceImpl : SettingApiService {
    override fun createSettingById(
        id: String,
        settingDTO: SettingDTO,
    ) {
        TODO("Not yet implemented")
    }

    override fun deleteSettingById(id: String) {
        TODO("Not yet implemented")
    }

    override fun getSettingById(id: String): SettingDTO {
        TODO("Not yet implemented")
    }

    override fun getSettings(): List<SettingDTO> {
        TODO("Not yet implemented")
    }

    override fun updateSettingById(
        id: String,
        settingDTO: SettingDTO,
    ) {
        TODO("Not yet implemented")
    }
}
