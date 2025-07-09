package com.fgbg.setting

import com.intellij.openapi.fileChooser.FileChooserDescriptor
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

    private fun createBrowseBtn(): TextFieldWithBrowseButton {
        val browseField = TextFieldWithBrowseButton()

        // 设置初始路径
        browseField.text = SettingsState.getInstance().filePath

        // 设置点击“浏览”按钮时的行为
        browseField.addBrowseFolderListener(
            "选择文件",               // 对话框标题
            null,                    // 对话框描述
            null,                    // 项目上下文（可为 null）
            FileChooserDescriptor(true, false, false, false, false, false) // 仅选择文件
        )

        return browseField
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