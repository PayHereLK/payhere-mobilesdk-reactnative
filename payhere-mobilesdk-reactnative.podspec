require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "payhere-mobilesdk-reactnative"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  PayHere SDK for React Native
                   DESC
  s.homepage     = "https://github.com/PayHereLK/payhere-mobilesdk-reactnative"
  s.license      = "MIT"
  s.authors      = { "PayHere" => "support@payhere.lk" }
  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/PayHereLK/payhere-mobilesdk-reactnative.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,c,m,swift}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency "payHereSDK", '= 2.1.1'
end

