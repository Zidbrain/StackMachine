package ui

import CompilationError
import Emulator
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import defaultStyle
import kotlinx.coroutines.flow.filterNotNull
import machine.toCommand
import java.util.*

@Composable
private fun Tooltip(text: String) {
    Box(modifier = Modifier.border(1.dp, Color.Black)) {
        Text(text, modifier = Modifier.background(Color.White).padding(4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ButtonWithTooltip(tooltip: String, image: String, onClick: () -> Unit) {
    TooltipArea(tooltip = { Tooltip(tooltip) }) {
        IconButton(modifier = Modifier.size(24.dp), onClick = onClick) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(resourcePath = image),
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
@Preview
fun MainScreen(programText: String, onProgramTextChanged: (String) -> Unit) {
    val lines = programText.lines()
    val emulator = remember { Emulator() }
    var alertMessage: String? by remember { mutableStateOf(null) }

    if (alertMessage != null)
        AlertDialog(onDismissRequest = { alertMessage = null },
            confirmButton = {
                TextButton(onClick = { alertMessage = null }) {
                    Text("OK")
                }
            },
            title = {
                Text("Ошибка")
            },
            text = {
                Text(alertMessage!!)
            }
        )

    LaunchedEffect(Unit) {
        snapshotFlow { emulator.compilationError }.filterNotNull().collect {
            alertMessage = when (it) {
                CompilationError.DataError -> "Ошибка в блоке DATA"
                is CompilationError.WrongLabelName -> "Ошибка на строке ${it.line}: LABEL не может иметь имя ${it.label}"
                is CompilationError.LineError -> "Ошибка на строке ${it.line}"
                CompilationError.NoProgramStart -> "Не найдена директива START"
                CompilationError.RuntimeError -> "Ошибка во время выполнения программы"
            }
        }
    }

    ProvideTextStyle(defaultStyle) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)
            ) {
                ButtonWithTooltip("Выполнить программу полностью", "images/play.svg") {
                    emulator.compileAndRun(lines)
                }
                ButtonWithTooltip("Выполнить следующую команду", "images/run-stop.svg") {
                    emulator.step(lines)
                }
                ButtonWithTooltip("Сбросить эмулятор", "images/reset.svg") {
                    emulator.reset()
                }
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(0.7f)) {
                    Box(modifier = Modifier.weight(1f)) {
                        val state = rememberScrollState()
                        BasicTextField(
                            modifier = Modifier.fillMaxHeight().verticalScroll(state),
                            value = programText,
                            onValueChange = { onProgramTextChanged(it.uppercase(Locale.getDefault())) },
                            decorationBox = {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                                ) {
                                    Column(
                                        modifier = Modifier.background(Color.White).fillMaxHeight()
                                            .width(IntrinsicSize.Min).padding(top = 8.dp),
                                    ) {
                                        repeat(lines.size) {
                                            val error = emulator.compilationError
                                            val color = when {
                                                (error is CompilationError.LineError && error.line == it) -> Color.Red
                                                emulator.currentCommand == it -> Color.Green
                                                else -> Color.Transparent
                                            }
                                            Text(
                                                text = it.toString(),
                                                modifier = Modifier.fillMaxWidth().background(color = color)
                                                    .padding(horizontal = 8.dp),
                                                textAlign = TextAlign.Center
                                            )

                                        }
                                    }
                                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(start = 8.dp, top = 8.dp)
                                    ) {
                                        it()
                                    }
                                }
                            }
                        )
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(state),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                    Divider()
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        with(emulator.machine) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Регистры")
                                Text("C: $counter")
                                Text("IP: $instructionPointer")
                                Text(
                                    "IR: 0x${
                                        instructionRegister.toHexString().padStart(2, '0')
                                    } (${instructionRegister.toCommand()})"
                                )
                                Text("SP: $stackPointer")
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Регистр флагов")
                                Text("EQ: ${flags.equal.toInt()}")
                                Text("Carry: ${flags.carry.toInt()}")
                                Text("Greater: ${flags.greater.toInt()}")
                                Text("Less: ${flags.less.toInt()}")
                            }
                        }
                    }
                }
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                MemoriesScreen(
                    modifier = Modifier.weight(0.3f),
                    machine = emulator.machine
                )
            }
        }
    }
}

private fun Boolean.toInt(): Int = if (this) 1 else 0
