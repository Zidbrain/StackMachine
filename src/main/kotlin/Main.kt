import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import ui.MainScreen
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

val defaultStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
)

@Composable
private fun FileDialog(
    parent: Frame? = null,
    mode: Int,
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", mode) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(directory + file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

fun main() = application {
    Window(state = rememberWindowState(size = DpSize(1200.dp, 900.dp)), title = "Эмулятор", onCloseRequest = ::exitApplication) {
        var fileHandle: String? by remember { mutableStateOf(null) }
        var openFile by remember { mutableStateOf(false) }
        var saveFile by remember { mutableStateOf(false) }
        var programText by remember { mutableStateOf("") }

        if (openFile) {
            FileDialog(
                mode = FileDialog.LOAD,
                onCloseRequest = {
                    it?.let {
                        fileHandle = it
                        programText = File(it).readText()
                    }
                    openFile = false
                }
            )
        }
        if (saveFile)
            FileDialog(
                mode = FileDialog.SAVE,
                onCloseRequest = {
                    it?.let {
                        fileHandle = it
                        val file = File(fileHandle!!)
                        file.writeText(programText)
                    }
                }
            )

        MenuBar {
            Menu("Файл") {
                Item("Окрыть", shortcut = KeyShortcut(Key.O, ctrl = true)) {
                    openFile = true
                }
                Item("Сохранить", shortcut = KeyShortcut(Key.S, ctrl = true)) {
                    if (fileHandle == null) saveFile = true
                    else {
                        val file = File(fileHandle!!)
                        file.writeText(programText)
                    }
                }
                Item("Сохранить как", shortcut = KeyShortcut(Key.S, ctrl = true, shift = true)) {
                    saveFile = true
                }
            }
        }
        MainScreen(programText = programText, onProgramTextChanged = { programText = it })
    }
}
