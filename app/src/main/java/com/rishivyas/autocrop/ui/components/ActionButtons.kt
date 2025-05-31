package com.rishivyas.autocrop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A 2x2 grid of action buttons for the camera screen
 */
@Composable
fun ActionButtons(
    onCaptureClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onCropClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    showCrop: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCaptureClick,
                modifier = Modifier
                    .width(160.dp)
                    .padding(horizontal = 4.dp)
            ) { 
                Text(
                    text = "Camera",
                    textAlign = TextAlign.Center,
                    maxLines = 1
                ) 
            }
            
            if (showCrop) {
                Button(
                    onClick = onCropClick,
                    modifier = Modifier
                        .width(160.dp)
                        .padding(horizontal = 4.dp)
                ) { 
                    Text(
                        text = "Crop Face",
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .width(160.dp)
                        .padding(horizontal = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onPickGalleryClick,
                modifier = Modifier
                    .width(160.dp)
                    .padding(horizontal = 4.dp)
            ) { 
                Text(
                    text = "Gallery",
                    textAlign = TextAlign.Center,
                    maxLines = 1
                ) 
            }
            
            Button(
                onClick = onViewHistoryClick,
                modifier = Modifier
                    .width(160.dp)
                    .padding(horizontal = 4.dp)
            ) { 
                Text(
                    text = "View History",
                    textAlign = TextAlign.Center,
                    maxLines = 1
                ) 
            }
        }
    }
} 