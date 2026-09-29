package com.imageforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.imageforge.app.ui.theme.ImageForgeTheme

class MainActivity: ComponentActivity(){ override fun onCreate(b:Bundle?){ super.onCreate(b); setContent{ ImageForgeTheme{ App() } } } }

data class Tool(val title:String,val subtitle:String,val icon: androidx.compose.ui.graphics.vector.ImageVector)
@Composable fun App(){
 var selected by remember{ mutableStateOf(0) }; var count by remember{ mutableIntStateOf(0) }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)){ count=it.size }
 Scaffold(containerColor=MaterialTheme.colorScheme.background,bottomBar={ NavigationBar { listOf("Home" to Icons.Outlined.Home,"Studio" to Icons.Outlined.Tune,"Batch" to Icons.Outlined.Collections,"Recipes" to Icons.Outlined.AutoAwesome,"Settings" to Icons.Outlined.Settings).forEachIndexed{i,p->NavigationBarItem(selected==i,{selected=i},{Icon(p.second,null)},label={Text(p.first)})} } }){ pad->
  when(selected){0->Home(Modifier.padding(pad),count){picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}; else->Placeholder(Modifier.padding(pad),listOf("Studio","Batch Studio","Recipes","Settings")[selected-1])}
 }
}
@Composable fun Home(m:Modifier,count:Int,pick:()->Unit){ Column(m.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
 Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("IMAGEFORGE",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text("Create the perfect image",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Compress, resize and prepare images without the guesswork.",color=MaterialTheme.colorScheme.onSurfaceVariant)};Surface(shape=RoundedCornerShape(16.dp),tonalElevation=2.dp){Icon(Icons.Outlined.Bolt,null,Modifier.padding(14.dp),tint=MaterialTheme.colorScheme.primary)}}
 Card(shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primary)){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Outlined.AddPhotoAlternate,null,tint=MaterialTheme.colorScheme.onPrimary);Text(if(count==0)"Start with your images" else "$count images ready",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimary);Text("Choose photos securely with Android Photo Picker.",color=MaterialTheme.colorScheme.onPrimary.copy(.8f));Button(pick,colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.surface,contentColor=MaterialTheme.colorScheme.primary)){Text(if(count==0)"Choose images" else "Choose different images")}}}
 Text("Quick tools",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
 val tools=listOf(Tool("Make smaller","Smart compression",Icons.Outlined.Compress),Tool("Fit upload limit","Target KB",Icons.Outlined.TrackChanges),Tool("Resize","Dimensions & presets",Icons.Outlined.AspectRatio),Tool("Convert","JPG · PNG · WebP",Icons.Outlined.SwapHoriz))
 tools.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{t->Card(Modifier.weight(1f),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Icon(t.icon,null,tint=MaterialTheme.colorScheme.primary);Text(t.title,fontWeight=FontWeight.SemiBold);Text(t.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
 }
}
@Composable fun Placeholder(m:Modifier,title:String){Box(m.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){Card(shape=RoundedCornerShape(28.dp)){Column(Modifier.padding(30.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.Construction,null,Modifier.size(42.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(12.dp));Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Foundation ready. Functional engine arrives in the next milestone.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
