require 'github/markup'


desc "Render README"
task :render do
  res = GitHub::Markup.render("README.textile", File.read("README.textile"))
  File.write("README.html", res)
end
