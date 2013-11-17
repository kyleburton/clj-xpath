task :default => ["jekyll:autogen"]

namespace :jekyll do
  desc "run jekyll in auto mode"
  task :autogen do
    Dir.chdir("src") do
      exec "jekyll serve -d site -w"
    end
  end

  desc "Build site."
  task :build do
    Dir.chdir("src") do
      exec "jekyll build -s src -d site"
    end
  end
end
