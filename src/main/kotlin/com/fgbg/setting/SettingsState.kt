package com.fgbg.setting

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "com.fgbg.editor.SettingsState",
    storages = [Storage("WriteYourThoughtSettings.xml")]
)
class SettingsState : PersistentStateComponent<SettingsState> {
    var filePath: String = ""

    override fun getState(): SettingsState = this

    override fun loadState(state: SettingsState) {
        filePath = state.filePath
    }

    companion object {
        fun getInstance(): SettingsState {
            return ApplicationManager.getApplication().getService(SettingsState::class.java)
        }
    }
}