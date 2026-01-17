// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.util.set
import com.tailscale.ipn.ui.viewModel.MeshyraLoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshyraLoginView(
    onNavigateHome: () -> Unit,
    viewModel: MeshyraLoginViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorDialog.collectAsState()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            
            // Error handling
            error?.let { 
                ErrorDialog(type = it, action = { viewModel.errorDialog.set(null) }) 
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Logo
                TailscaleLogoView(modifier = Modifier.size(64.dp))
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Meshyra Client", // Or Meshyra Client if we updated strings
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "安全的点对点网络连接工具",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Login Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Tabs (Account Login / Scan Login) - Visual only for now as requested scan is removed?
                        // User request: "但是没有 扫码登入栏". 
                        // Implementation: Just show Account Login form.

                        var username by remember { mutableStateOf("") }
                        var password by remember { mutableStateOf("") }
                        var captcha by remember { mutableStateOf("") }
                        val focusManager = LocalFocusManager.current

                        // Username
                        Column {
                            Text("用户名", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = { Text("请输入用户名") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }

                        // Password
                        Column {
                            Text("密码", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("请输入密码") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }

                        // Captcha
                        Column {
                            Text("验证码", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = captcha,
                                    onValueChange = { captcha = it },
                                    placeholder = { Text("请输入验证码") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { 
                                        focusManager.clearFocus()
                                        viewModel.performLogin(username, password, captcha, onNavigateHome)
                                    })
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                // Placeholder for captcha image - using a simple box or text button for now
                                Box(
                                    modifier = Modifier
                                        .height(56.dp)
                                        .background(Color.LightGray, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("AB3X", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Login Button
                        PrimaryActionButton(
                            onClick = { 
                                focusManager.clearFocus()
                                viewModel.performLogin(username, password, captcha, onNavigateHome)
                            },
                        ) {
                             if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("登录")
                            }
                        }
                    }
                }
            }
        }
    }
}
