package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun MemoryDump(data: List<UShort>, pointer: UShort, hex: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val state = rememberLazyListState()
        LazyColumn(state = state) {
            itemsIndexed(data) { i, it ->
                Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.background(
                            color = if (i % 2 == 0) Color.Cyan else Color.LightGray
                        ).fillMaxWidth()
                    ) {
                        Text(String.format("%04d", i), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Box(
                            modifier = Modifier
                                .background(color = Color.Gray)
                                .fillMaxHeight()
                                .width(1.dp)
                        )
                        val text = if (hex) it.toHexString().padStart(4, '0') else it.toString().padStart(4, '0')
                        Text(text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }

                    if (pointer.toInt() == i)
                        Image(
                            painter = painterResource(resourcePath = "images/right-arrow.svg"),
                            contentDescription = null,
                            alignment = Alignment.CenterStart,
                            modifier = Modifier
                                .matchParentSize()
                                .padding(2.dp)
                        )
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(state),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}