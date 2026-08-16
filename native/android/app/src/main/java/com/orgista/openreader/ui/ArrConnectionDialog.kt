package com.orgista.openreader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ArrConnectionDialog(
    ebookServerUrl: String,
    audiobookServerUrl: String,
    loading: Boolean,
    connected: Boolean,
    onDismiss: () -> Unit,
    onConnect: (String, String, String, String) -> Unit,
    onDisconnect: () -> Unit,
) {
    var ebookUrl by remember(ebookServerUrl) { mutableStateOf(ebookServerUrl) }
    var audiobookUrl by remember(audiobookServerUrl) { mutableStateOf(audiobookServerUrl) }
    var ebookApiKey by remember { mutableStateOf("") }
    var audiobookApiKey by remember { mutableStateOf("") }
    var showKeys by remember { mutableStateOf(false) }
    val submit = { onConnect(ebookUrl, ebookApiKey, audiobookUrl, audiobookApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Hub, contentDescription = null) },
        title = { Text(if (connected) "ARR managers" else "Connect ARR managers") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Ebook manager")
                OutlinedTextField(
                    value = ebookUrl,
                    onValueChange = { ebookUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server address") },
                    singleLine = true,
                    enabled = !loading,
                )
                OutlinedTextField(
                    value = ebookApiKey,
                    onValueChange = { ebookApiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKeys = !showKeys }) {
                            Icon(
                                if (showKeys) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showKeys) "Hide API keys" else "Show API keys",
                            )
                        }
                    },
                )
                Text("Audiobook manager", modifier = Modifier.padding(top = 3.dp))
                OutlinedTextField(
                    value = audiobookUrl,
                    onValueChange = { audiobookUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server address") },
                    singleLine = true,
                    enabled = !loading,
                )
                OutlinedTextField(
                    value = audiobookApiKey,
                    onValueChange = { audiobookApiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                if (connected) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        OutlinedButton(onClick = onDisconnect, enabled = !loading) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = submit,
                enabled = !loading && ebookUrl.isNotBlank() && audiobookUrl.isNotBlank() &&
                    ebookApiKey.isNotBlank() && audiobookApiKey.isNotBlank(),
            ) {
                Text(if (loading) "Connecting" else "Connect")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !loading) {
                Text("Cancel")
            }
        },
    )
}
