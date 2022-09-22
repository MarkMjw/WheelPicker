package sh.tyy.wheelpicker

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import sh.tyy.wheelpicker.core.*
import sh.tyy.wheelpicker.databinding.TriplePickerViewBinding
import java.lang.ref.WeakReference

class CityPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TripleDependentPickerView(context, attrs, defStyleAttr), BaseWheelPickerView.WheelPickerViewListener {

    enum class Mode {
        PROVINCE_CITY_AREA,
        PROVINCE_CITY,
        PROVINCE,
    }

    var mode: Mode = Mode.PROVINCE_CITY_AREA
        set(value) {
            if (field == value) {
                return
            }
            field = value
            when (value) {
                Mode.PROVINCE_CITY_AREA -> {
                    areaPickerView.visibility = View.VISIBLE
                    cityPickerView.visibility = View.VISIBLE
                }
                Mode.PROVINCE_CITY -> {
                    cityPickerView.visibility = View.VISIBLE
                    areaPickerView.visibility = View.GONE
                    setThird(1, false, null)
                }
                Mode.PROVINCE -> {
                    areaPickerView.visibility = View.GONE
                    cityPickerView.visibility = View.GONE
                    setSecond(0, false, null)
                    setThird(1, false, null)
                }
            }
        }

    override val adapters: Triple<RecyclerView.Adapter<*>, RecyclerView.Adapter<*>, RecyclerView.Adapter<*>>
        get() = Triple(provinceAdapter, cityAdapter, areaAdapter)

    override val currentData: TripleDependentData
        get() = TripleDependentData(province, city, area)

    override fun minData(): TripleDependentData? = null

    override fun maxData(): TripleDependentData? = null

    override fun value(adapter: RecyclerView.Adapter<*>, valueIndex: Int): Int {
        return valueIndex
    }

    interface Listener {
        fun didSelectData(year: Int, month: Int, day: Int)
    }

    private val highlightView: View = run {
        val view = View(context)
        view.background = ContextCompat.getDrawable(context, R.drawable.text_wheel_highlight_bg)
        view
    }
    private val provincePickerView: TextWheelPickerView
    private val cityPickerView: TextWheelPickerView
    private val areaPickerView: TextWheelPickerView

    private var listener: Listener? = null

    fun setWheelListener(listener: Listener) {
        this.listener = listener
    }

    val province: Int
        get() = provincePickerView.selectedIndex

    val city: Int
        get() = cityPickerView.selectedIndex

    val area: Int
        get() = areaPickerView.selectedIndex

    private val provinces: MutableList<String> = mutableListOf()
    private val cities: MutableList<List<String>> = mutableListOf()
    private val areas: MutableList<List<List<String>>> = mutableListOf()

    fun setData(
        provinces: List<String>,
        cities: List<List<String>>,
        areas: List<List<List<String>>> = emptyList()
    ) {
        this.provinces.clear()
        this.provinces.addAll(provinces)

        this.cities.clear()
        this.cities.addAll(cities)

        this.areas.clear()
        this.areas.addAll(areas)

        provinceAdapter.values = provinces.mapIndexed { index, s -> TextWheelPickerView.Item("$index", s) }
    }

    fun setCity(province: Int, city: Int, area: Int) {
        setFirst(province, false) {
            setSecond(city, false) {
                setThird(area, false, null)
            }
        }
    }

    private val provinceAdapter = ItemEnableWheelAdapter(WeakReference(this))
    private val cityAdapter = ItemEnableWheelAdapter(WeakReference(this))
    private val areaAdapter = ItemEnableWheelAdapter(WeakReference(this))

    private val binding: TriplePickerViewBinding =
        TriplePickerViewBinding.inflate(LayoutInflater.from(context), this)

    override fun setFirst(value: Int, animated: Boolean, completion: (() -> Unit)?) {
        if (this.province == value) {
            completion?.invoke()
            return
        }
        provincePickerView.setSelectedIndex(value, animated, completion)
    }

    override fun setSecond(value: Int, animated: Boolean, completion: (() -> Unit)?) {
        if (this.city == value) {
            completion?.invoke()
            return
        }
        cityPickerView.setSelectedIndex(value, animated, completion)
    }

    override fun setThird(value: Int, animated: Boolean, completion: (() -> Unit)?) {
        if (this.area == value) {
            completion?.invoke()
            return
        }
        areaPickerView.setSelectedIndex(value, animated, completion)
    }

    override fun setHapticFeedbackEnabled(hapticFeedbackEnabled: Boolean) {
        super.setHapticFeedbackEnabled(hapticFeedbackEnabled)
        provincePickerView.isHapticFeedbackEnabled = hapticFeedbackEnabled
        cityPickerView.isHapticFeedbackEnabled = hapticFeedbackEnabled
        areaPickerView.isHapticFeedbackEnabled = hapticFeedbackEnabled
    }

    init {
        provincePickerView = binding.leftPicker
        provincePickerView.isCircular = false
        provincePickerView.setAdapter(provinceAdapter)

        cityPickerView = binding.midPicker
        cityPickerView.isCircular = false
        cityPickerView.setAdapter(cityAdapter)

        areaPickerView = binding.rightPicker
        areaPickerView.isCircular = false
        areaPickerView.setAdapter(areaAdapter)

        addView(highlightView,
            0,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.resources.getDimensionPixelSize(R.dimen.text_wheel_picker_item_height)).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        )

        provincePickerView.setWheelListener(this)
        cityPickerView.setWheelListener(this)
        areaPickerView.setWheelListener(this)
    }

    private fun updateCityPickerViewIfNeeded(index: Int): Boolean {
        val cityList = cities.getOrNull(index) ?: return false
        cityAdapter.values = cityList.mapIndexed { i, s -> TextWheelPickerView.Item("$i", s) }
        setSecond(0, true, null)
        cityPickerView.post {
            cityPickerView.refreshCurrentPosition()
        }
        return true
    }

    private fun updateAreaPickerViewIfNeeded(province: Int, city: Int): Boolean {
        val areaList = areas.getOrNull(province)?.getOrNull(city) ?: return false
        areaAdapter.values = areaList.mapIndexed { i, s -> TextWheelPickerView.Item("$i", s) }
        setThird(0, true, null)
        areaPickerView.post {
            areaPickerView.refreshCurrentPosition()
        }
        return true
    }

    private fun dateIsValid(data: TripleDependentData): Boolean {
        if (data.first == NO_POSITION && data.second == NO_POSITION && data.third == NO_POSITION) {
            return false
        }
        val format = "%04d%02d%02d"
        val selectedDateString = format.format(data.first, data.second, data.third)
        minData()?.let {
            if (selectedDateString < format.format(it.first, it.second, it.third)) {
                return false
            }
        }
        maxData()?.let {
            if (selectedDateString > format.format(it.first, it.second, it.third)) {
                return false
            }
        }
        return true
    }

    // region BaseWheelPickerView.WheelPickerViewListener
    override fun didSelectItem(picker: BaseWheelPickerView, index: Int) {
        if (picker == provincePickerView) {
            updateCityPickerViewIfNeeded(index)
        } else if (picker == cityPickerView) {
            updateAreaPickerViewIfNeeded(province, index)
        }

        if (!dateIsValid(currentData)) {
            return
        }
        listener?.didSelectData(province, city, area)
    }

    override fun onScrollStateChanged(state: Int) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            updateCurrentDataByDataRangeIfNeeded(true)
        }
    }
    // endregion
}