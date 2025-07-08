package com.fgbg.action

import com.fgbg.setting.SettingsState
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.OnOffButton
import org.intellij.plugins.markdown.ui.preview.MarkdownEditorWithPreview
import org.intellij.plugins.markdown.ui.preview.MarkdownPreviewFileEditor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

class WriteThoughtAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val filePath = SettingsState.getInstance().filePath
        if (filePath.isEmpty()) {
            Messages.showErrorDialog("错误!", "请先设置打开文件路径")
            return
        }

        LocalFileSystem.getInstance().findFileByPath(filePath)?.let {
            MarkdownDialogWrapper(project, it).show()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

class MarkdownDialogWrapper(
    private val project: Project,
    private val file: VirtualFile
) : DialogWrapper(project) {
    private lateinit var markdownEditor: MarkdownPreviewFileEditor
    private lateinit var psiAwareTextEditorImpl: PsiAwareTextEditorImpl
    private lateinit var markdownEditorWithPreview: MarkdownEditorWithPreview
    private val pc = PropertiesComponent.getInstance()

    private lateinit var comp: JComponent

    init {
        title = "Markdown Preview"
        setOKButtonText("Close")
        setSize(800, 600)
        isResizable = true
        init()
    }

    override fun createCenterPanel(): JComponent {
        psiAwareTextEditorImpl = PsiAwareTextEditorImpl(project, file, TextEditorProvider()).apply {
            editor.settings.isUseSoftWraps = pc.getValue(LINE_WRAP_KEY)?.toBoolean() ?: false
        }

        markdownEditor = MarkdownPreviewFileEditor(project, file)
        markdownEditorWithPreview = MarkdownEditorWithPreview(psiAwareTextEditorImpl, markdownEditor)

        val base = EditorColorsManager.getInstance().globalScheme.defaultBackground
        val alpha = pc.getValue(TRANSPARENCY_KEY)?.toIntOrNull() ?: 100
        comp = markdownEditorWithPreview.component.apply {
            isOpaque = false
            background = Color(base.red, base.green, base.blue, alpha)
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            background = null
            add(comp, BorderLayout.CENTER)
        }
    }

    override fun createNorthPanel(): JComponent {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            background = null
        }

        // 透明度滑块
        val alphaSlider = JSlider(0, 255, pc.getValue(TRANSPARENCY_KEY)?.toIntOrNull() ?: 100).apply {
            majorTickSpacing = 50
            minorTickSpacing = 10
            paintTicks = true
            paintLabels = true
            toolTipText = "背景透明度"
            maximumSize = Dimension(200, 50)

            addChangeListener {
                val alpha = value
                val base = EditorColorsManager.getInstance().globalScheme.defaultBackground
                comp.background = Color(base.red, base.green, base.blue, alpha)
                pc.setValue(TRANSPARENCY_KEY, alpha.toString())
                comp.repaint()
            }
        }

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
            pc.getValue(LINE_WRAP_KEY)?.let {
                isSelected = it.toBoolean()
            }

            addActionListener {
                psiAwareTextEditorImpl.editor.settings.isUseSoftWraps = isSelected
                pc.setValue(LINE_WRAP_KEY, isSelected.toString())
            }

            onText = "自动换行"
            offText = "取消换行"
        }

        panel.add(Box.createHorizontalStrut(10))
        panel.add(JLabel("透明度: "))
        panel.add(alphaSlider)
        panel.add(Box.createHorizontalStrut(20))
        panel.add(settingBtn)
        panel.add(Box.createHorizontalStrut(10))
        panel.add(onOffButton)
        return panel
    }

    override fun dispose() {
        super.dispose()
        Disposer.dispose(markdownEditor)
        Disposer.dispose(psiAwareTextEditorImpl)
        Disposer.dispose(markdownEditorWithPreview)
    }

    companion object {
        private const val LINE_WRAP_KEY = "markdown.dialog.wrap"
        private const val TRANSPARENCY_KEY = "markdown.dialog.transparency"
    }
}