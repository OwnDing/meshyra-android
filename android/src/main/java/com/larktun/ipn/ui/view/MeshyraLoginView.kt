// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.larktun.ipn.ui.view

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.larktun.ipn.R
import com.larktun.ipn.ui.util.set
import com.larktun.ipn.ui.viewModel.MeshyraLoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshyraLoginView(
    onNavigateHome: () -> Unit,
    viewModel: MeshyraLoginViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingMessageRes by viewModel.loadingMessageRes.collectAsState()
    val isCaptchaLoading by viewModel.isCaptchaLoading.collectAsState()
    val captchaState by viewModel.captcha.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentLocale = LocalConfiguration.current.locales[0]
    val isChinese = currentLocale.language.equals("zh", ignoreCase = true)
    val captchaBitmap by
        produceState<ImageBitmap?>(initialValue = null, key1 = captchaState?.imageBase64) {
          val dataUri = captchaState?.imageBase64.orEmpty()
          val base64 = dataUri.substringAfter(',', missingDelimiterValue = "")
          value =
              if (base64.isBlank()) null
              else {
                withContext(Dispatchers.Default) {
                  runCatching {
                        val decoded = Base64.decode(base64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
                      }
                      .getOrNull()
                }
              }
        }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            val scrollState = rememberScrollState()
            
            // Error handling
            errorMessage?.let { message ->
                ErrorDialog(message = message, onDismiss = { viewModel.errorMessage.set(null) })
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Logo
                TailscaleLogoView(modifier = Modifier.size(64.dp))
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.meshyra_client_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = stringResource(R.string.meshyra_client_subtitle),
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

                        var username by rememberSaveable { mutableStateOf("") }
                        var password by rememberSaveable { mutableStateOf("") }
                        var captcha by rememberSaveable { mutableStateOf("") }
                        val focusManager = LocalFocusManager.current

                        // Username
                        Column {
                            Text(
                                stringResource(R.string.meshyra_login_username_label),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                enabled = !isLoading,
                                placeholder = {
                                    Text(
                                        text =
                                            stringResource(R.string.meshyra_login_username_placeholder),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }

                        // Password
                        Column {
                            Text(
                                stringResource(R.string.meshyra_login_password_label),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                enabled = !isLoading,
                                placeholder = {
                                    Text(
                                        text =
                                            stringResource(R.string.meshyra_login_password_placeholder),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }

                        // Captcha
                        Column {
                            Text(
                                stringResource(R.string.meshyra_login_captcha_label),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = captcha,
                                    onValueChange = { captcha = it },
                                    enabled = !isLoading,
                                    placeholder = {
                                        Text(
                                            text =
                                                stringResource(
                                                    R.string.meshyra_login_captcha_placeholder
                                                ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { 
                                        focusManager.clearFocus()
                                        viewModel.performLogin(username, password, captcha, onNavigateHome)
                                    })
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.LightGray)
                                        .clickable(enabled = !isCaptchaLoading && !isLoading) {
                                            viewModel.refreshCaptcha()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    captchaBitmap?.let { bitmap ->
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = stringResource(R.string.meshyra_login_captcha_image),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.FillBounds,
                                        )
                                    }
                                    if (isCaptchaLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp,
                                        )
                                    }
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
                            enabled = !isLoading,
                        ) {
                             if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.log_in))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            TextButton(
                enabled = !isLoading,
                onClick = {
                    val tags = if (isChinese) "en" else "zh-Hans"
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
                },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .zIndex(1f),
            ) {
                Text(
                    text =
                        stringResource(
                            if (isChinese) R.string.language_toggle_to_english
                            else R.string.language_toggle_to_chinese
                        ),
                )
            }

            if (isLoading) {
                MeshyraLoginLoadingOverlay(
                    message =
                        stringResource(
                            loadingMessageRes ?: R.string.meshyra_login_loading_connecting
                        )
                )
            }
        }
    }
}

@Composable
private fun MeshyraLoginLoadingOverlay(message: String) {
    val transition = rememberInfiniteTransition(label = "meshyraLoginLoading")
    val pulseScale by
        transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
            label = "meshyraLoginPulseScale",
        )
    val pulseAlpha by
        transition.animateFloat(
            initialValue = 0.16f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
            label = "meshyraLoginPulseAlpha",
        )
    val dots by produceState(initialValue = "") {
        while (true) {
            for (count in 0..3) {
                value = ".".repeat(count)
                delay(320)
            }
        }
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(104.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(84.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                                    shape = CircleShape,
                                ),
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                    )
                    TailscaleLogoView(animated = true, modifier = Modifier.size(44.dp))
                }

                Text(
                    text = stringResource(R.string.meshyra_login_loading_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message + dots,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.meshyra_login_loading_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
