package com.fgbg.action

import com.fgbg.setting.SettingsConfigurable
import com.fgbg.setting.SettingsState
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableEP
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.OnOffButton
import org.intellij.plugins.markdown.ui.preview.MarkdownEditorWithPreview
import org.intellij.plugins.markdown.ui.preview.MarkdownPreviewFileEditor
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class WriteThoughtAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val filePath = SettingsState.getInstance().filePath
        if (filePath.isEmpty()) return

        LocalFileSystem.getInstance().findFileByPath(filePath)?.let {
            MarkdownDialogWrapper(project, it).show()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

const val LINE_WRAP_KEY = "lineWrap"

class MarkdownDialogWrapper(private val project: Project, private val file: VirtualFile) : DialogWrapper(project) {
    private lateinit var markdownEditor: MarkdownPreviewFileEditor
    private lateinit var psiAwareTextEditorImpl: PsiAwareTextEditorImpl
    private lateinit var markdownEditorWithPreview: MarkdownEditorWithPreview
    private val pc = PropertiesComponent.getInstance()

    init {
        title = "Markdown Preview"
        setOKButtonText("Close")
        setSize(800, 600)
        isResizable = true
        init()
    }

    override fun createCenterPanel(): JComponent {
        psiAwareTextEditorImpl = PsiAwareTextEditorImpl(project, file, TextEditorProvider()).apply {
            // 读取配置
            editor.settings.isUseSoftWraps = pc.getValue(LINE_WRAP_KEY)?.toBoolean() ?: false
        }
        markdownEditor = MarkdownPreviewFileEditor(project, file)
        markdownEditorWithPreview = MarkdownEditorWithPreview(psiAwareTextEditorImpl, markdownEditor)

        val comp = markdownEditorWithPreview.component
        return comp
    }

    override fun createNorthPanel(): JComponent {
        val northPanel = JPanel()
        val settingBtn = JButton("设置", AllIcons.General.Settings).apply {
            addMouseListener(object : MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Write Your Thought Settings")
                }

                override fun mousePressed(e: MouseEvent?) = Unit
                override fun mouseReleased(e: MouseEvent?) = Unit
                override fun mouseEntered(e: MouseEvent?) = Unit
                override fun mouseExited(e: MouseEvent?) = Unit
            })
        }
        val onOffButton = OnOffButton().apply {

            // 读取设置
            pc.getValue(LINE_WRAP_KEY)?.let {
                isSelected = it.toBoolean()
            }

            // 监听设置变化
            addActionListener {
                psiAwareTextEditorImpl.editor.settings.isUseSoftWraps = isSelected

                // 持久化设置
                pc.apply {
                    setValue(LINE_WRAP_KEY, isSelected.toString())
                }

            }
            onText = "自动换行"
            offText = "取消换行"
        }
        northPanel.add(settingBtn)
        northPanel.add(onOffButton)
        return northPanel
    }

    override fun dispose() {
        super.dispose()
        Disposer.dispose(markdownEditor)
        Disposer.dispose(psiAwareTextEditorImpl)
        Disposer.dispose(markdownEditorWithPreview)
    }
}