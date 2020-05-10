require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = 'react-native-braintree-xplat'
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platform     = :ios, "9.0"

  s.source       = { :git => "https://github.com/bbdev/react-native-braintree-xplat.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/**/*.{h,m}"

  s.dependency 'Braintree'
  s.dependency 'BraintreeDropIn'
  s.dependency 'Braintree/PayPal'
  s.dependency 'Braintree/Venmo'
  s.dependency 'Braintree/Apple-Pay'
  s.dependency 'Braintree/3D-Secure'
  s.dependency 'Braintree/DataCollector'
  s.dependency 'React'
end
