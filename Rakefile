task :default => ["jekyll:autogen"]

namespace :jekyll do
  desc "run jekyll in auto mode"
  task :autogen do
    Dir.chdir("src") do
      exec "jekyll --auto --server 4444"
    end
  end
end
