package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import machine.StackMachine

@Composable
fun MemoriesScreen(modifier: Modifier = Modifier, machine: StackMachine) = with(machine) {
    Column(modifier) {
        var hex by remember { mutableStateOf(true) }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Регистровый стек", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Divider()
                MemoryDump(stack, stackPointer, hex, modifier)
            }
            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(color = Color.Gray))
            Column(modifier = Modifier.weight(1f)) {
                Text("ОЗУ", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Divider()
                MemoryDump(memory, instructionPointer, hex)
            }
        }

        Divider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hex", modifier = Modifier.padding(start = 8.dp))
            Checkbox(checked = hex, onCheckedChange = { hex = it })
        }
    }
}