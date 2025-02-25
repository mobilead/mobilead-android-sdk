package com.kakao.adfit.publisher.sample

import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import com.kakao.adfit.ads.AdError
import com.kakao.adfit.ads.na.AdFitAdInfoIconPosition
import com.kakao.adfit.ads.na.AdFitNativeAdBinder
import com.kakao.adfit.ads.na.AdFitNativeAdLayout
import com.kakao.adfit.ads.na.AdFitNativeAdLoader
import com.kakao.adfit.ads.na.AdFitNativeAdRequest
import com.kakao.adfit.ads.na.AdFitNativeAdView
import com.kakao.adfit.ads.na.AdFitVideoAutoPlayPolicy

class NativeAdSampleActivity : AppCompatActivity(), AdFitNativeAdLoader.AdLoadListener {

    private val adUnitId: String = "발급받은 광고단위 ID" // FIXME: 발급받은 광고단위 ID를 입력해주세요.

    private lateinit var nativeAdFrameLayout: FrameLayout
    private lateinit var loadAdButton: Button

    private var nativeAdLayout: AdFitNativeAdLayout? = null

    private var nativeAdLoader: AdFitNativeAdLoader? = null
    private var nativeAdBinder: AdFitNativeAdBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_native_ad_smaple)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)

            WindowInsetsCompat.CONSUMED
        }

        nativeAdFrameLayout = findViewById(R.id.nativeAdFrameLayout)

        loadAdButton = findViewById(R.id.loadAdButton)
        loadAdButton.setOnClickListener {
            loadNativeAd()
        }

        // [AdFitNativeAdLoader] 생성
        nativeAdLoader = AdFitNativeAdLoader.create(this, adUnitId)
    }

    override fun onDestroy() {
        nativeAdBinder?.unbind() // 노출 중인 광고가 있으면 해제
        nativeAdBinder = null

        nativeAdLoader = null

        super.onDestroy()
    }

    /**
     * 새로운 네이티브 광고를 요청합니다.
     */
    private fun loadNativeAd() {
        if (nativeAdLoader == null) {
            return
        }

        if (TextUtils.equals(adUnitId, "발급받은 광고단위 ID")) {
            Toast.makeText(this, "광고단위 ID를 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // (샘플 구현용) 광고 요청 버튼 비활성화 (동시 요청 제한)
        loadAdButton.isEnabled = false

        // 네이티브 광고 요청 정보 설정
        val request = AdFitNativeAdRequest.Builder()

            /**
             * 광고 정보 아이콘 위치를 설정합니다.
             *
             * 광고 정보 아이콘은 [AdFitNativeAdView] 내 설정한 위치에 노출됩니다.
             *
             * 기본값: [AdFitAdInfoIconPosition.RIGHT_TOP] 우상단
             *
             * @see [AdFitAdInfoIconPosition.LEFT_TOP] 좌상단
             * @see [AdFitAdInfoIconPosition.RIGHT_TOP] 우상단
             * @see [AdFitAdInfoIconPosition.LEFT_BOTTOM] 좌하단
             * @see [AdFitAdInfoIconPosition.RIGHT_BOTTOM] 우하단
             */
            .setAdInfoIconPosition(AdFitAdInfoIconPosition.RIGHT_TOP) // 광고 정보 아이콘 위치 설정 (container view 내에서의 광고 아이콘 위치)

            /**
             * 비디오 광고 자동 재생 정책을 설정합니다.
             *
             * 기본값: [AdFitVideoAutoPlayPolicy.WIFI_ONLY] WiFi 연결 상태에서만 자동재생
             *
             * @see [AdFitVideoAutoPlayPolicy.ALWAYS] 항상 자동재생
             * @see [AdFitVideoAutoPlayPolicy.WIFI_ONLY] WiFi 연결 상태에서만 자동재생
             * @see [AdFitVideoAutoPlayPolicy.NONE] 자동 재생하지 않음
             */
            .setVideoAutoPlayPolicy(AdFitVideoAutoPlayPolicy.WIFI_ONLY) // 비디오 광고 자동 재생 정책 설정
            .build()

        /**
         * 새로운 네이티브 광고를 요청합니다.
         *
         * 요청에 성공하여 새로운 광고를 전달받은 경우,
         * [AdFitNativeAdLoader.AdLoadListener.onAdLoaded] 콜백을 통해 광고 소재를 전달받습니다.
         *
         * 요청에 실패하거나 전달받은 광고가 없을 경우,
         * [AdFitNativeAdLoader.AdLoadListener.onAdLoadError] 콜백을 통해 오류코드를 전달받습니다.
         *
         * 동시에 하나의 요청만 처리할 수 있으며,
         * 이전 요청이 진행 중이면 새로운 요청은 무시됩니다.
         */
        nativeAdLoader?.loadAd(request, this)
    }

    /**
     * 광고 요청 성공 콜백
     *
     * 요청에 성공하여 소재를 응답받았을 때 호출됩니다.
     *
     * @param binder 광고 소재 정보를 갖고 있는 [AdFitNativeAdBinder]
     */
    @UiThread
    override fun onAdLoaded(binder: AdFitNativeAdBinder) {
        if (nativeAdLoader == null ||
            lifecycle.currentState == Lifecycle.State.DESTROYED
        ) {
            // [Activity]가 먼저 종료된 경우, 메모리 누수(Memory Leak) 및 오류를 방지를 위해 응답을 무시
            return
        }

        var nativeAdLayout = nativeAdLayout
        if (nativeAdLayout == null) {
            val nativeAdView = layoutInflater.inflate(R.layout.item_native_ad, nativeAdFrameLayout, false)
            nativeAdFrameLayout.addView(nativeAdView)

            nativeAdLayout = AdFitNativeAdLayout.Builder(nativeAdView.findViewById(R.id.containerView)) // 네이티브 광고 영역 (광고 아이콘이 배치 됩니다)
                .setTitleView(nativeAdView.findViewById(R.id.titleTextView)) // 광고 제목 (필수)
                .setBodyView(nativeAdView.findViewById(R.id.bodyTextView)) // 광고 홍보문구
                .setProfileIconView(nativeAdView.findViewById(R.id.profileIconView)) // 광고주 아이콘 (브랜드 로고)
                .setProfileNameView(nativeAdView.findViewById(R.id.profileNameTextView)) // 광고주 이름 (브랜드명)
                .setMediaView(nativeAdView.findViewById(R.id.mediaView)) // 광고 미디어 소재 (이미지, 비디오) (필수)
                .setCallToActionButton(nativeAdView.findViewById(R.id.callToActionButton)) // 행동유도버튼 (알아보기, 바로가기 등)
                .build()

            this.nativeAdLayout = nativeAdLayout
        } else {
            // 이전에 노출 중인 광고가 있으면 해제
            this.nativeAdBinder?.unbind()
        }

        // 광고 노출
        nativeAdBinder = binder
        binder.bind(nativeAdLayout)

        // (샘플 구현용) 광고 요청 버튼 활성화
        loadAdButton.isEnabled = true
    }

    /**
     * 광고 요청 실패 콜백
     *
     * 요청에 실패하거나 전달 받은 광고가 없을 때 호출됩니다.
     *
     * @param errorCode 광고 요청에서 발생한 오류코드
     *
     * @see [com.kakao.adfit.ads.AdError]
     */
    @UiThread
    override fun onAdLoadError(errorCode: Int) {
        if (nativeAdLoader == null ||
            lifecycle.currentState == Lifecycle.State.DESTROYED
        ) {
            // [Activity]가 먼저 종료된 경우, 오류를 방지를 위해 무시
            return
        }

        // TODO: 요청 실패 처리
        when (errorCode) {
            AdError.NO_AD.errorCode -> {
                // 요청에는 성공했으나 노출 가능한 광고가 없는 경우
            }

            AdError.HTTP_FAILED.errorCode -> {
                // 네트워크 오류로 광고 요청에 실패한 경우
            }

            else -> {
                // 기타 오류로 광고 요청에 실패한 경우
            }
        }

        if (nativeAdBinder == null) {
            // TODO: 보여지고 있는 광고가 없을 때의 처리
        } else {
            // TODO: 보여지고 있는 광고가 있을 때의 처리
        }

        // (샘플 구현용) 광고 요청 버튼 활성화
        loadAdButton.isEnabled = true
    }
}
