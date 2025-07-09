package com.fgbg.action

import com.fgbg.setting.SettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColorUtil
import com.intellij.ui.jcef.JCEFHtmlPanel
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JPanel

class WriteThoughtAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val filePath = SettingsState.getInstance().filePath
        if (filePath.isEmpty()) {
            Messages.showErrorDialog("请先设置打开文件路径", "错误!")
            return
        }

        LocalFileSystem.getInstance().findFileByPath(filePath)?.let {
            MarkdownWindow(project, it)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

class MarkdownWindow(val project: Project, val file: VirtualFile) : JDialog() {
    private val jcefHtmlPanel: JCEFHtmlPanel = JCEFHtmlPanel("url")
    private val myComponent: JComponent = JPanel()

    init {
        val fileContent = file.inputStream.bufferedReader().readText()
        // 读取模板内容, 写入Vditor框架
        val html = javaClass.classLoader.getResourceAsStream("template/template.html")?.use { stream ->
            stream.bufferedReader().readText()
                .replace("{{value}}", fileContent)
                .apply {
                    val theme = getVditorTheme()
                    replace("{{theme}}", theme)
                    if (theme == "dark") {
                        replace("{{style}}", ".vditor--dark{--panel-background-color:rgba(43,43,43,1.00);--textarea-background-color:rgba(43,43,43,1.00);--toolbar-background-color:rgba(60,63,65,1.00);}::-webkit-scrollbar-track {background-color:rgba(43,43,43,1.00);}::-webkit-scrollbar-thumb {background-color:rgba(166,166,166,0.28);}.vditor-reset {font-size:16px;font-family:\"JetBrains Mono\",\"Helvetica Neue\",\"Luxi Sans\",\"DejaVu Sans\",\"Hiragino Sans GB\",\"Microsoft Yahei\",sans-serif,\"Apple Color Emoji\",\"Segoe UI Emoji\",\"Noto Color Emoji\",\"Segoe UI Symbol\",\"Android Emoji\",\"EmojiSymbols\";color:rgba(169,183,198,1.00);} body{background-color: rgba(43,43,43,1.00);}.vditor-reset a {color: rgba(30, 136, 234);}")
                    } else {
                        replace("{{style}}", "")
                    }
                }
        } ?: "<html><body><h1>Template not found</h1></body></html>"

        jcefHtmlPanel.loadHTML(html)
        add(jcefHtmlPanel.component)
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    fun getVditorTheme(): String {
        // 获取当前编辑器的颜色方案
        val colorsManager = EditorColorsManager.getInstance()
        val globalScheme = colorsManager.globalScheme

        // 直接获取默认背景颜色
        val defaultBackground = globalScheme.defaultBackground

        // 判断主题类型
        val vditorTheme = if (ColorUtil.isDark(defaultBackground)) {
            "dark"
        } else {
            "light"
        }
        return vditorTheme
    }
}
