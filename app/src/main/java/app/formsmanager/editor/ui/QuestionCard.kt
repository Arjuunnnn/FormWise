package app.formsmanager.editor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropDownCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.UpdateQuestionDescription
import app.formsmanager.editor.SetShuffle
import app.formsmanager.editor.SetScaleRange
import app.formsmanager.editor.UpdateScaleLabel
import app.formsmanager.editor.SetDateOptions
import app.formsmanager.editor.SetDuration
import app.formsmanager.editor.SetPoints
import app.formsmanager.editor.ToggleCorrectAnswer
import app.formsmanager.editor.SetCorrectText
import app.formsmanager.editor.AddOption
import app.formsmanager.editor.AddOtherOption
import app.formsmanager.editor.DeleteQuestion
import app.formsmanager.editor.DuplicateQuestion
import app.formsmanager.editor.EditCommand
import app.formsmanager.editor.MoveOption
import app.formsmanager.editor.OptionDraft
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.QuestionType
import app.formsmanager.editor.RemoveOption
import app.formsmanager.editor.SetRequired
import app.formsmanager.editor.UpdateOption
import app.formsmanager.editor.UpdateQuestionTitle
import sh.calvin.reorderable.ReorderableColumn

private val CardShape = FormsShapes.Card
private val TitleStyle: TextStyle get() = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = FormsColors.Ink)
private val OptionStyle: TextStyle get() = TextStyle(fontSize = 16.sp, color = FormsColors.Ink)

@Composable
fun QuestionCard(
    question: QuestionDraft,
    expanded: Boolean,
    onFocus: () -> Unit,
    onPickType: () -> Unit,
    onCommand: (EditCommand) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    dragHandle: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** True when the form is a quiz: shows points and the answer key. */
    quiz: Boolean = false,
) {
    val borderColor by animateColorAsState(
        targetValue = if (expanded) FormsColors.Cyan else Color.Transparent,
        label = "card-border",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(CardShape)
            .background(FormsColors.Surface)
            .border(3.dp, borderColor, CardShape)
            .clickable(enabled = !expanded, onClick = onFocus)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeHeader(question.type, expanded, onPickType)
            Spacer(Modifier.weight(1f))
            dragHandle?.invoke()
        }

        if (expanded) {
            PlainField(
                value = question.title,
                onValueChange = { onCommand(UpdateQuestionTitle(question.id, it)) },
                placeholder = "Question",
                textStyle = TitleStyle,
            )
            PlainField(
                value = question.description,
                onValueChange = { onCommand(UpdateQuestionDescription(question.id, it)) },
                placeholder = "Description (optional)",
                textStyle = OptionStyle.copy(color = FormsColors.InkMuted, fontSize = 15.sp),
            )
            if (question.type.hasOptions) {
                OptionList(question, quiz, onCommand)
            }
            when (question.type) {
                QuestionType.SCALE -> ScaleSettings(question, onCommand)
                QuestionType.DATE -> DateSettings(question, onCommand)
                QuestionType.TIME -> TimeSettings(question, onCommand)
                QuestionType.SHORT_ANSWER -> if (quiz) {
                    PlainField(
                        value = question.correctAnswers.firstOrNull().orEmpty(),
                        onValueChange = { onCommand(SetCorrectText(question.id, it)) },
                        placeholder = "Correct answer (optional)",
                        textStyle = OptionStyle,
                    )
                }
                else -> Unit
            }
            CardFooter(question, quiz, onMoveUp, onMoveDown, onCommand)
        } else {
            Text(
                text = question.title.ifBlank { "Untitled question" } + if (question.required) " *" else "",
                style = TitleStyle,
                color = if (question.title.isBlank()) FormsColors.InkMuted else FormsColors.Ink,
            )
            if (quiz && question.type.canGrade && question.points > 0) {
                Text(
                    "${question.points} ${if (question.points == 1) "point" else "points"}",
                    color = FormsColors.CyanDeep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun TypeHeader(type: QuestionType, expanded: Boolean, onPickType: () -> Unit) {
    Row(
        modifier = Modifier.clickable(enabled = expanded, onClick = onPickType),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(type.icon(), contentDescription = null, tint = FormsColors.Ink)
        Text(type.label(), style = OptionStyle, fontWeight = FontWeight.Bold)
        if (expanded) {
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Change question type", tint = FormsColors.InkMuted)
        }
    }
}

@Composable
private fun OptionList(question: QuestionDraft, quiz: Boolean, onCommand: (EditCommand) -> Unit) {
    Column {
        ReorderableColumn(
            list = question.options,
            onSettle = { from, to ->
                onCommand(MoveOption(question.id, from, to))
            },
        ) { index, option, isDragging ->
            ReorderableItem {
                Column {
                    HorizontalDivider(color = FormsColors.Outline)
                    OptionRow(
                        index = index,
                        type = question.type,
                        option = option,
                        canRemove = question.options.size > 1,
                        isLastOption = index == question.options.lastIndex,
                        showCorrect = quiz && !option.isOther,
                        correct = option.text in question.correctAnswers,
                        onToggleCorrect = { onCommand(ToggleCorrectAnswer(question.id, option.text)) },
                        onTextChange = { onCommand(UpdateOption(question.id, option.id, it)) },
                        onRemove = { onCommand(RemoveOption(question.id, option.id)) },
                        onAddNext = { onCommand(AddOption(question.id)) },
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .draggableHandle(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.DragHandle, contentDescription = "Reorder option", tint = FormsColors.InkMuted)
                            }
                        },
                    )
                }
            }
        }
        HorizontalDivider(color = FormsColors.Outline)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onCommand(AddOption(question.id)) }) {
                Text("Add option", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
            }
            val canAddOther = question.type != QuestionType.DROPDOWN && question.options.none { it.isOther }
            if (canAddOther) {
                Text("or", color = FormsColors.InkMuted)
                TextButton(onClick = { onCommand(AddOtherOption(question.id)) }) {
                    Text("Add “Other”", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    index: Int,
    type: QuestionType,
    option: OptionDraft,
    canRemove: Boolean,
    isLastOption: Boolean,
    showCorrect: Boolean,
    correct: Boolean,
    onToggleCorrect: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    onAddNext: () -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OptionMarker(type, index)
        if (option.isOther) {
            Text("Other…", style = OptionStyle.copy(color = FormsColors.InkMuted), modifier = Modifier.weight(1f))
        } else {
            PlainField(
                value = option.text,
                onValueChange = onTextChange,
                placeholder = "Option",
                textStyle = OptionStyle,
                imeAction = if (isLastOption) ImeAction.Done else ImeAction.Next,
                onImeAction = if (isLastOption) onAddNext else null,
                modifier = Modifier.weight(1f),
            )
        }
        if (showCorrect) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (correct) FormsColors.Cyan else Color.Transparent)
                    .border(2.dp, if (correct) FormsColors.Cyan else FormsColors.Hairline, CircleShape)
                    .clickable(onClick = onToggleCorrect),
                contentAlignment = Alignment.Center,
            ) {
                if (correct) Icon(Icons.Outlined.Check, contentDescription = "Correct answer", tint = FormsColors.OnCyan, modifier = Modifier.size(18.dp))
            }
        }
        if (canRemove) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove option", tint = FormsColors.Ink)
            }
        } else {
            Spacer(Modifier.width(32.dp))
        }
        dragHandle()
    }
}

@Composable
private fun OptionMarker(type: QuestionType, index: Int) {
    when (type) {
        QuestionType.MULTIPLE_CHOICE -> Icon(Icons.Outlined.RadioButtonUnchecked, null, tint = FormsColors.Ink)
        QuestionType.CHECKBOXES -> Icon(Icons.Outlined.CheckBoxOutlineBlank, null, tint = FormsColors.Ink)
        else -> Text("${index + 1}.", style = OptionStyle, modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun CardFooter(
    question: QuestionDraft,
    quiz: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onCommand: (EditCommand) -> Unit,
) {
    val buttonSize = Modifier.size(40.dp)
    Column {
    if (question.type.hasOptions) {
        SwitchRow("Shuffle option order", question.shuffle) { onCommand(SetShuffle(question.id, it)) }
    }
    if (quiz && question.type.canGrade) {
        StepperRow("Points", question.points, 0, 100) { onCommand(SetPoints(question.id, it)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onCommand(DuplicateQuestion(question.id)) }, modifier = buttonSize) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate question", tint = FormsColors.Ink)
        }
        IconButton(onClick = { onCommand(DeleteQuestion(question.id)) }, modifier = buttonSize) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete question", tint = FormsColors.Ink)
        }
        if (onMoveUp != null) {
            IconButton(onClick = onMoveUp, modifier = buttonSize) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up", tint = FormsColors.Ink)
            }
        }
        if (onMoveDown != null) {
            IconButton(onClick = onMoveDown, modifier = buttonSize) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down", tint = FormsColors.Ink)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Required", style = OptionStyle, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = question.required,
            onCheckedChange = { onCommand(SetRequired(question.id, it)) },
            colors = formSwitchColors(),
        )
    }
    }
}

@Composable
private fun formSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = FormsColors.Cyan,
    checkedThumbColor = FormsColors.OnCyan,
    checkedBorderColor = FormsColors.Cyan,
    uncheckedTrackColor = FormsColors.Track,
    uncheckedThumbColor = FormsColors.Ink,
    uncheckedBorderColor = FormsColors.Track,
)

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = OptionStyle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = formSwitchColors())
    }
}

@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = OptionStyle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Remove, contentDescription = "Decrease $label", tint = FormsColors.Ink)
        }
        Text(
            value.toString(),
            style = OptionStyle,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "Increase $label", tint = FormsColors.Ink)
        }
    }
}

@Composable
private fun PickChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        style = OptionStyle,
        fontWeight = FontWeight.ExtraBold,
        color = if (selected) FormsColors.OnCyan else FormsColors.Ink,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) FormsColors.Cyan else Color.Transparent)
            .border(1.dp, if (selected) FormsColors.Cyan else FormsColors.Hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ScaleSettings(question: QuestionDraft, onCommand: (EditCommand) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("From", style = OptionStyle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            PickChip("0", question.scaleLow == 0) { onCommand(SetScaleRange(question.id, 0, question.scaleHigh)) }
            PickChip("1", question.scaleLow == 1) { onCommand(SetScaleRange(question.id, 1, question.scaleHigh)) }
        }
        StepperRow("To", question.scaleHigh, 2, 10) { onCommand(SetScaleRange(question.id, question.scaleLow, it)) }
        HorizontalDivider(color = FormsColors.Outline)
        PlainField(
            value = question.scaleLowLabel,
            onValueChange = { onCommand(UpdateScaleLabel(question.id, high = false, text = it)) },
            placeholder = "Label for ${question.scaleLow} (optional)",
            textStyle = OptionStyle,
        )
        PlainField(
            value = question.scaleHighLabel,
            onValueChange = { onCommand(UpdateScaleLabel(question.id, high = true, text = it)) },
            placeholder = "Label for ${question.scaleHigh} (optional)",
            textStyle = OptionStyle,
        )
    }
}

@Composable
private fun DateSettings(question: QuestionDraft, onCommand: (EditCommand) -> Unit) {
    Column {
        SwitchRow("Include year", question.includeYear) { onCommand(SetDateOptions(question.id, it, question.includeTime)) }
        SwitchRow("Include time", question.includeTime) { onCommand(SetDateOptions(question.id, question.includeYear, it)) }
    }
}

@Composable
private fun TimeSettings(question: QuestionDraft, onCommand: (EditCommand) -> Unit) {
    SwitchRow("Ask for a duration instead of a time of day", question.duration) { onCommand(SetDuration(question.id, it)) }
}

@Composable
internal fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    placeholderColor: Color = FormsColors.InkMuted,
    cursorBrush: Brush = SolidColor(FormsColors.Ink),
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        cursorBrush = cursorBrush,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction?.invoke() }, onDone = { onImeAction?.invoke() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = placeholderColor))
                }
                inner()
            }
        },
    )
}

internal fun QuestionType.label() = when (this) {
    QuestionType.SHORT_ANSWER -> "Short answer"
    QuestionType.PARAGRAPH -> "Paragraph"
    QuestionType.MULTIPLE_CHOICE -> "Multiple choice"
    QuestionType.CHECKBOXES -> "Checkboxes"
    QuestionType.DROPDOWN -> "Dropdown"
    QuestionType.SCALE -> "Linear scale"
    QuestionType.DATE -> "Date"
    QuestionType.TIME -> "Time"
}

internal fun QuestionType.icon(): ImageVector = when (this) {
    QuestionType.SHORT_ANSWER -> Icons.AutoMirrored.Outlined.ShortText
    QuestionType.PARAGRAPH -> Icons.AutoMirrored.Outlined.Notes
    QuestionType.MULTIPLE_CHOICE -> Icons.Outlined.RadioButtonChecked
    QuestionType.CHECKBOXES -> Icons.Outlined.CheckBox
    QuestionType.DROPDOWN -> Icons.Outlined.ArrowDropDownCircle
    QuestionType.SCALE -> Icons.Outlined.LinearScale
    QuestionType.DATE -> Icons.Outlined.CalendarMonth
    QuestionType.TIME -> Icons.Outlined.Schedule
}
