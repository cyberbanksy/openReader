package com.orgista.openreader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

private enum class ConnectionMethod(val label: String) {
    Password("Password"),
    ApiKey("API key"),
}

@Composable
fun ConnectionDialog(
    serverUrl: String,
    username: String,
    loading: Boolean,
    connected: Boolean,
    onDismiss: () -> Unit,
    onConnect: (String, String, String, Boolean) -> Unit,
    onDisconnect: () -> Unit,
) {
    var editedServer by remember(serverUrl) { mutableStateOf(serverUrl) }
    var editedUsername by remember(username) { mutableStateOf(username) }
    var credential by remember { mutableStateOf("") }
    var showCredential by remember { mutableStateOf(false) }
    var method by remember(username) {
        mutableStateOf(if (username == "API key") ConnectionMethod.ApiKey else ConnectionMethod.Password)
    }
    val submit = {
        onConnect(editedServer, editedUsername, credential, method == ConnectionMethod.ApiKey)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
        title = { Text(if (connected) "Server" else "Connect your library") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editedServer,
                    onValueChange = { editedServer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server address") },
                    singleLine = true,
                    enabled = !loading,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ConnectionMethod.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = method == option,
                            onClick = {
                                method = option
                                credential = ""
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ConnectionMethod.entries.size),
                            enabled = !loading,
                            label = { Text(option.label) },
                        )
                    }
                }
                if (method == ConnectionMethod.Password) {
                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = { editedUsername = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        singleLine = true,
                        enabled = !loading,
                    )
                }
                OutlinedTextField(
                    value = credential,
                    onValueChange = { credential = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(method.label) },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = if (showCredential) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCredential = !showCredential }) {
                            Icon(
                                if (showCredential) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showCredential) "Hide ${method.label}" else "Show ${method.label}",
                            )
                        }
                    },
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
                enabled = !loading &&
                    editedServer.isNotBlank() &&
                    credential.isNotBlank() &&
                    (method == ConnectionMethod.ApiKey || editedUsername.isNotBlank()),
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
