Pod::Spec.new do |s|
  s.name             = 'KtorBatteries'
  s.version          = '0.1.1'
  s.summary          = 'KtorBatteries'

  s.description      = <<-DESC
  Helpers for connecting to a Ktor server
                       DESC

  s.homepage         = 'https://github.com/lightningkite/ktor-batteries'
  # s.screenshots     = 'www.example.com/screenshots_1', 'www.example.com/screenshots_2'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'Joseph' => 'joseph@lightningkite.com' }
  s.source           = { :git => 'https://github.com/lightningkite/ktor-batteries.git', :tag => s.version.to_s }
  # s.social_media_url = 'https://twitter.com/<TWITTER_USERNAME>'

  s.ios.deployment_target = '11.0'

  s.source_files = 'ios/KtorBatteries/Classes/**/*'
  s.dependency 'RxSwiftPlus/Http'
  s.dependency 'KhrysalisRuntime'
end
