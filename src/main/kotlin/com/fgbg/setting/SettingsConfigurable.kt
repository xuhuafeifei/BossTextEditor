package com.fgbg.setting

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.OnOffButton
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class SettingsConfigurable : Configurable {
    private lateinit var browseButton: TextFieldWithBrowseButton
    override fun getDisplayName(): String = "Write Your Thought Settings"

    override fun createComponent(): JComponent {
        browseButton = createBrowseBtn()

        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("读取文件路径", browseButton)
            .panel

        return panel
    }

    private fun createBrowseBtn(): TextFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        SettingsState.getInstance().filePath.let { text = it }
    }


    override fun isModified(): Boolean {
        val settings = SettingsState.getInstance()
        return browseButton.text != settings.filePath
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val settings = SettingsState.getInstance()
        settings.filePath = browseButton.text
    }

    override fun reset() {
        val settings = SettingsState.getInstance()
        browseButton.text = settings.filePath
    }
}