package com.fgbg.action

import com.fgbg.setting.SettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBSplitter
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.Timer

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

class MarkdownWindow(val project: Project, val file: VirtualFile) : JDialog(){
    private val jcefHtmlPanel: JCEFHtmlPanel = JCEFHtmlPanel("url")
    private lateinit var timer: Timer
    val jsQuery = JBCefJSQuery.create(jcefHtmlPanel as JBCefBrowserBase)


    init {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE)

        val fileContent = file.inputStream.bufferedReader().readText().replace("`", "\\`")
        // 读取模板内容, 写入Vditor框架
        var html = javaClass.classLoader.getResourceAsStream("template/template.html")?.use { stream ->
            stream.bufferedReader().readText()
                .replace("{{value}}", fileContent)
        } ?: "<html><body><h1>Template not found</h1></body></html>"

        val theme = getVditorTheme()
        html = html.replace("{{theme}}", theme)
        if (theme == "dark") {
            html = html.replace("{{css}}", getDarkCss())
        } else {
            html = html.replace("{{css}}", "")
        }

        // 注册回调处理函数
        jsQuery.addHandler { message ->
            runWriteCommand(project) {
                file.setBinaryContent(message.toByteArray())
            }

            // 可返回给 JS 的响应
            JBCefJSQuery.Response("保存成功")
        }

        jcefHtmlPanel.loadHTML(html)

        val injectJS = """
        window.javaBridge = {
            postMessage: function(msg) {
                ${jsQuery.inject("msg")}
            }
        };
        console.log("✅ 注入 javaBridge 完成");
        """.trimIndent()

        jcefHtmlPanel.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    browser?.executeJavaScript(injectJS, browser.url, 0)
                    timer = Timer(1000 * 10, {
                        jcefHtmlPanel.cefBrowser.executeJavaScript(
                            "saveContent()",
                            jcefHtmlPanel.cefBrowser.url,
                            42
                        )
                    })
                    timer.start()
                }
            }
        }, jcefHtmlPanel.cefBrowser)

        val splitter = JBSplitter(true, 0.95f)
        splitter.firstComponent = jcefHtmlPanel.component
        splitter.secondComponent = JBSplitter(false, 0.5f)

        add(jcefHtmlPanel.component)
        pack()
        size = Dimension( 500, 400)
        setLocationRelativeTo(null)
        isVisible = true
    }

    fun runWriteCommand(project: Project, runnable: () -> Unit) {
        WriteCommandAction.runWriteCommandAction(project, runnable)
    }


    private fun getDarkCss(): String {
        return "<style id=\"ideaStyle\">.vditor--dark{--panel-background-color:rgba(43,43,43,1.00);--textarea-background-color:rgba(43,43,43,1.00);--toolbar-background-color:rgba(60,63,65,1.00);}::-webkit-scrollbar-track {background-color:rgba(43,43,43,1.00);}::-webkit-scrollbar-thumb {background-color:rgba(166,166,166,0.28);}.vditor-reset {font-size:16px;font-family:\"JetBrains Mono\",\"Helvetica Neue\",\"Luxi Sans\",\"DejaVu Sans\",\"Hiragino Sans GB\",\"Microsoft Yahei\",sans-serif,\"Apple Color Emoji\",\"Segoe UI Emoji\",\"Noto Color Emoji\",\"Segoe UI Symbol\",\"Android Emoji\",\"EmojiSymbols\";color:rgba(169,183,198,1.00);} body{background-color: rgba(43,43,43,1.00);}.vditor-reset a {color: rgba(30, 136, 234);}</style>"
    }

    fun getVditorTheme(): String {
        // 获取当前编辑器的颜色方案
        val colorsManager = EditorColorsManager.getInstance()
        val globalScheme = colorsManager.globalScheme

        // 直接获取默认背景颜色
        val defaultBackground = globalScheme.defaultBackground

        // 判断主题类型
        return if (ColorUtil.isDark(defaultBackground)) {
            "dark"
        } else {
            "light"
        }
    }

    override fun dispose() {
        timer.stop()
        super.dispose()
    }
}
