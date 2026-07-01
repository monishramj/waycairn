package com.waycairn.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.waycairn.WaycairnApp

/** Pulls the [WaycairnApp] (which holds the repositories) out of a ViewModel factory's extras. */
fun CreationExtras.waycairnApp(): WaycairnApp = this[APPLICATION_KEY] as WaycairnApp
